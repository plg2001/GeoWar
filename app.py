
import os
import time
import random
import math

from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from werkzeug.security import generate_password_hash, check_password_hash

from google.oauth2 import id_token
from google.auth.transport import requests as google_requests

# ---------------- APP & DB ----------------

app = Flask(__name__)

DB_URI = os.getenv(
    "DATABASE_URL",
    "mysql+pymysql://Plg2001:mydatabase@Plg2001.mysql.pythonanywhere-services.com/Plg2001$default?charset=utf8mb4",
)

app.config["SQLALCHEMY_DATABASE_URI"] = DB_URI
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config["SQLALCHEMY_ENGINE_OPTIONS"] = {
    "pool_pre_ping": True,
    "pool_recycle": 280,
}

db = SQLAlchemy(app)

ALLOWED_TEAMS = {"RED", "BLUE"}
LOBBY_MAX_PLAYERS = 20
LOBBY_TEAM_SIZE = 10


# ---------------- MODELS ----------------

class Lobby(db.Model):
    __tablename__ = "lobby"
    id = db.Column(db.Integer, primary_key=True)
    status = db.Column(db.String(20), default="WAITING")  # WAITING, ACTIVE, FINISHED
    created_at = db.Column(db.Float, default=time.time)

    # CONTATORI RICHIESTI
    player_count = db.Column(db.Integer, default=0)
    targets_red = db.Column(db.Integer, default=0)
    targets_blue = db.Column(db.Integer, default=0)


class User(db.Model):
    __tablename__ = "user"

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False, index=True)
    email = db.Column(db.String(120), unique=True, nullable=False, index=True)
    password_hash = db.Column(db.String(255), nullable=False)

    team = db.Column(db.String(10), default=None)  # RED / BLUE
    score = db.Column(db.Integer, default=0)

    lat = db.Column(db.Float, default=0.0)
    lon = db.Column(db.Float, default=0.0)
    last_active = db.Column(db.Float, default=0.0)

    avatar_seed = db.Column(db.String(80), default="51d70032-0099-43b4-b2dd-5557")
    admin = db.Column(db.Boolean, default=False)
    banned = db.Column(db.Boolean, default=False)

    lobby_id = db.Column(db.Integer, db.ForeignKey("lobby.id"), nullable=True)


class Target(db.Model):
    __tablename__ = "target"

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(80), nullable=False)
    lat = db.Column(db.Float, nullable=False)
    lon = db.Column(db.Float, nullable=False)

    owner_team = db.Column(db.String(10), default="NEUTRAL")  # NEUTRAL/RED/BLUE
    last_hacked = db.Column(db.Float, default=0.0)

    lobby_id = db.Column(db.Integer, db.ForeignKey("lobby.id"), nullable=True)


class HackLog(db.Model):
    __tablename__ = "hack_log"

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey("user.id"), nullable=False)
    target_id = db.Column(db.Integer, db.ForeignKey("target.id"), nullable=False)

    team = db.Column(db.String(10), nullable=False)
    timestamp = db.Column(db.Float, default=time.time, nullable=False)


class PvPEncounter(db.Model):
    __tablename__ = "pvp_encounter"

    id = db.Column(db.Integer, primary_key=True)
    attacker_id = db.Column(db.Integer, db.ForeignKey("user.id"), nullable=False)
    defender_id = db.Column(db.Integer, db.ForeignKey("user.id"), nullable=False)

    winner_id = db.Column(db.Integer, nullable=True)
    timestamp = db.Column(db.Float, default=time.time, nullable=False)


class Stun(db.Model):
    __tablename__ = "stun"

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey("user.id"), nullable=False)
    until = db.Column(db.Float, nullable=False)  # time.time() + 120


# ---------------- HELPERS ----------------

def get_json():
    data = request.get_json(silent=True)
    return data if isinstance(data, dict) else None


def db_commit_or_error():
    """Ritorna None se ok, altrimenti (response, code)."""
    try:
        db.session.commit()
        return None
    except IntegrityError as e:
        db.session.rollback()
        return jsonify({"message": "Errore di integrità database", "detail": str(e)}), 409
    except SQLAlchemyError as e:
        db.session.rollback()
        return jsonify({"message": "Errore database", "detail": str(e)}), 500


def lobby_recount_targets(lobby_id: int):
    """Ricalcola i contatori target per una lobby (utile come safety net)."""
    lobby = Lobby.query.get(lobby_id)
    if not lobby:
        return

    red = Target.query.filter_by(lobby_id=lobby_id, owner_team="RED").count()
    blue = Target.query.filter_by(lobby_id=lobby_id, owner_team="BLUE").count()

    lobby.targets_red = red
    lobby.targets_blue = blue
    db.session.commit()


def lobby_adjust_target_counters(lobby: Lobby, old_owner: str, new_owner: str):
    """Aggiorna contatori su cambio ownership del target."""
    if not lobby:
        return

    if old_owner == new_owner:
        return

    # decremento vecchio owner
    if old_owner == "RED":
        lobby.targets_red = max(0, lobby.targets_red - 1)
    elif old_owner == "BLUE":
        lobby.targets_blue = max(0, lobby.targets_blue - 1)

    # incremento nuovo owner
    if new_owner == "RED":
        lobby.targets_red += 1
    elif new_owner == "BLUE":
        lobby.targets_blue += 1


def generate_lobby_targets(lobby_id: int, count: int = 20):
    """Genera target random per una lobby in bounding box Italia, owner NEUTRAL."""
    lobby = Lobby.query.get(lobby_id)
    if not lobby:
        return

    # Italia (approx bbox)
    min_lat, max_lat = 36.6, 47.1
    min_lon, max_lon = 6.6, 18.5

    new_targets = []
    for i in range(count):
        lat = random.uniform(min_lat, max_lat)
        lon = random.uniform(min_lon, max_lon)
        new_targets.append(
            Target(
                name=f"Obiettivo Lobby {lobby_id} #{i+1}",
                lat=lat,
                lon=lon,
                owner_team="NEUTRAL",
                lobby_id=lobby_id,
            )
        )

    db.session.add_all(new_targets)

    # reset contatori target lobby (tutti neutrali)
    lobby.targets_red = 0
    lobby.targets_blue = 0

    err = db_commit_or_error()
    return err


# ---------------- INIT ----------------

with app.app_context():
    db.create_all()

    # seed target globali se non esistono
    if not Target.query.first():
        sample_targets = [
            Target(name="Duomo di Milano", lat=45.4641, lon=9.1919, owner_team="NEUTRAL", lobby_id=None),
            Target(name="Castello Sforzesco", lat=45.4705, lon=9.1793, owner_team="NEUTRAL", lobby_id=None),
            Target(name="Stazione Centrale", lat=45.4859, lon=9.2035, owner_team="NEUTRAL", lobby_id=None),
            Target(name="Arco della Pace", lat=45.4754, lon=9.1724, owner_team="NEUTRAL", lobby_id=None),
            Target(name="Politecnico di Milano", lat=45.4790, lon=9.2274, owner_team="NEUTRAL", lobby_id=None),
        ]
        db.session.add_all(sample_targets)
        db.session.commit()


# ---------------- ROUTES ----------------

@app.route("/")
def home():
    return jsonify({"status": "API online", "message": "GeoWar Server Ready"}), 200


# ---------- AUTH ----------

@app.route("/register", methods=["POST"])
def register():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON mancante o non valido"}), 400

    username = (data.get("username") or "").strip()
    email = (data.get("email") or "").strip().lower()
    password = data.get("password")

    if not username or not email or not password:
        return jsonify({"message": "Dati mancanti (username, password, email)"}), 400

    if User.query.filter_by(username=username).first():
        return jsonify({"message": "Username già esistente"}), 400
    if User.query.filter_by(email=email).first():
        return jsonify({"message": "Email già registrata"}), 400

    new_user = User(
        username=username,
        email=email,
        password_hash=generate_password_hash(password),
    )

    db.session.add(new_user)
    err = db_commit_or_error()
    if err:
        return err

    return jsonify({"message": "Registrazione completata", "user": {"id": new_user.id, "username": new_user.username}}), 200


@app.route("/login", methods=["POST"])
def login():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON mancante o non valido"}), 400

    username = (data.get("username") or "").strip()
    password = data.get("password")

    if not username or not password:
        return jsonify({"message": "Dati mancanti (username, password)"}), 400

    user = User.query.filter_by(username=username).first()
    if not user:
        return jsonify({"message": "Credenziali errate"}), 401
    if user.banned:
        return jsonify({"message": "Utente bannato"}), 403

    if not check_password_hash(user.password_hash, password):
        return jsonify({"message": "Credenziali errate"}), 401

    return jsonify({
        "message": "Login effettuato",
        "user": {
            "id": user.id,
            "username": user.username,
            "team": user.team,
            "score": user.score,
            "admin": user.admin,
            "lobby_id": user.lobby_id
        },
        "token": "fake-jwt-token-12345"
    }), 200


@app.route("/auth/google_login", methods=["POST"])
def google_login():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON mancante o non valido"}), 400

    token = data.get("token")
    if not token:
        return jsonify({"message": "Token mancante"}), 400

    try:
        CLIENT_ID = os.getenv(
            "GOOGLE_CLIENT_ID",
            "143510152058-65kf5bucon42l77e7qk1bsgl70qki9so.apps.googleusercontent.com"
        )

        idinfo = id_token.verify_oauth2_token(token, google_requests.Request(), CLIENT_ID)

        email = (idinfo.get("email") or "").strip().lower()
        name = (idinfo.get("name") or "Utente Google").strip()

        if not email:
            return jsonify({"message": "Token valido ma email mancante"}), 400

        user = User.query.filter_by(email=email).first()
        if user:
            if user.banned:
                return jsonify({"message": "Utente bannato"}), 403
            return jsonify({
                "message": "Login effettuato",
                "user": {
                    "id": user.id,
                    "username": user.username,
                    "team": user.team,
                    "score": user.score,
                    "admin": user.admin,
                    "lobby_id": user.lobby_id
                }
            }), 200

        base_username = name.replace(" ", "_").lower() or "utente_google"
        new_username = base_username
        counter = 1
        while User.query.filter_by(username=new_username).first():
            new_username = f"{base_username}_{counter}"
            counter += 1

        new_user = User(
            username=new_username,
            email=email,
            password_hash=generate_password_hash(os.urandom(16).hex()),
        )

        db.session.add(new_user)
        err = db_commit_or_error()
        if err:
            return err

        return jsonify({
            "message": "Utente creato e login effettuato",
            "user": {
                "id": new_user.id,
                "username": new_user.username,
                "team": new_user.team,
                "score": new_user.score,
                "admin": new_user.admin,
                "lobby_id": new_user.lobby_id
            }
        }), 200

    except ValueError:
        return jsonify({"message": "Token Google non valido"}), 401
    except Exception as e:
        db.session.rollback()
        return jsonify({"message": f"Errore interno del server: {str(e)}"}), 500


# ---------- USER ----------

@app.route("/user/<int:user_id>", methods=["GET"])
def get_user_details(user_id):
    user = User.query.get(user_id)
    if not user:
        return jsonify({"message": "Utente non trovato"}), 404
    if user.banned:
        return jsonify({"message": "Utente bannato"}), 403

    return jsonify({
        "id": user.id,
        "username": user.username,
        "email": user.email,
        "team": user.team,
        "score": user.score,
        "lat": user.lat,
        "lon": user.lon,
        "last_active": user.last_active,
        "avatar_seed": user.avatar_seed,
        "admin": user.admin,
        "lobby_id": user.lobby_id
    }), 200


@app.route("/user/<int:user_id>", methods=["PUT"])
def update_user_details(user_id):
    data = get_json()
    if not data:
        return jsonify({"message": "JSON mancante o non valido"}), 400

    user = User.query.get(user_id)
    if not user:
        return jsonify({"message": "Utente non trovato"}), 404
    if user.banned:
        return jsonify({"message": "Utente bannato"}), 403

    new_username = (data.get("username") or "").strip()
    new_email = (data.get("email") or "").strip().lower()

    if not new_username or not new_email:
        return jsonify({"message": "Username e email non possono essere vuoti"}), 400

    if User.query.filter(User.id != user_id, User.username == new_username).first():
        return jsonify({"message": "Username già in uso"}), 409
    if User.query.filter(User.id != user_id, User.email == new_email).first():
        return jsonify({"message": "Email già in uso"}), 409

    user.username = new_username
    user.email = new_email

    new_avatar_seed = data.get("avatar_seed")
    if new_avatar_seed:
        user.avatar_seed = new_avatar_seed.strip()

    err = db_commit_or_error()
    if err:
        return err

    return jsonify({"message": "Profilo aggiornato con successo"}), 200


# ---------- LOBBIES ----------

@app.route("/lobbies", methods=["GET"])
def get_lobbies():
    lobbies = Lobby.query.filter(Lobby.status.in_(['WAITING', 'ACTIVE'])).all()
    result = []
    for lobby in lobbies:
        result.append({
            "id": lobby.id,
            "status": lobby.status,
            "created_at": lobby.created_at,
            "player_count": lobby.player_count,
            "targets_red": lobby.targets_red,
            "targets_blue": lobby.targets_blue,
        })
    return jsonify(result), 200


@app.route("/lobby/join", methods=["POST"])
def join_lobby():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON mancante"}), 400

    user_id = data.get("user_id")
    lobby_id_raw = data.get("lobby_id")

    if not user_id:
        return jsonify({"message": "User ID mancante"}), 400
    if lobby_id_raw is None:
        return jsonify({"message": "Lobby ID mancante"}), 400

    try:
        lobby_id = int(lobby_id_raw)
    except (ValueError, TypeError):
        return jsonify({"message": "Lobby ID non valido"}), 400

    user = User.query.get(user_id)
    if not user or user.banned:
        return jsonify({"message": "Utente non valido"}), 403

    target_lobby = Lobby.query.get(lobby_id)
    if not target_lobby:
        return jsonify({"message": "Lobby non trovata"}), 404

    if target_lobby.status not in ['WAITING', 'ACTIVE']:
        return jsonify({"message": "La lobby non è disponibile"}), 403

    if user.lobby_id:
        if user.lobby_id == lobby_id:
            return jsonify({"message": "Già in questa lobby", "lobby_id": str(target_lobby.id), "team": user.team}), 200
        return jsonify({"message": "Lascia la lobby corrente prima di unirtene ad un'altra"}), 400

    if target_lobby.player_count >= LOBBY_MAX_PLAYERS:
        return jsonify({"message": "Lobby piena"}), 409

    user.lobby_id = target_lobby.id
    user.team = None  # reset team
    target_lobby.player_count += 1

    err = db_commit_or_error()
    if err:
        return err

    return jsonify({"message": "Lobby assegnata", "lobby_id": str(target_lobby.id)}), 200


@app.route("/lobby/leave", methods=["POST"])
def leave_lobby():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON mancante"}), 400

    user_id = data.get("user_id")
    if not user_id:
        return jsonify({"message": "User ID mancante"}), 400

    user = User.query.get(user_id)
    if not user:
        return jsonify({"message": "Utente non trovato"}), 404

    lobby_id = user.lobby_id
    if not lobby_id:
        return jsonify({"message": "Utente non è in una lobby", "success": True}), 200

    lobby = Lobby.query.get(lobby_id)

    # rimuovi utente
    user.lobby_id = None
    user.team = None

    if lobby:
        lobby.player_count = max(0, lobby.player_count - 1)

    err = db_commit_or_error()
    if err:
        return err

    # se era ACTIVE: verifica requisiti minimi (1 per team)
    lobby = Lobby.query.get(lobby_id)
    if lobby and lobby.status == "ACTIVE":
        red_count = User.query.filter_by(lobby_id=lobby_id, team="RED").count()
        blue_count = User.query.filter_by(lobby_id=lobby_id, team="BLUE").count()

        if red_count < 1 or blue_count < 1:
            lobby.status = "WAITING"

            # cancella target lobby + reset contatori
            Target.query.filter_by(lobby_id=lobby_id).delete()
            lobby.targets_red = 0
            lobby.targets_blue = 0

            err2 = db_commit_or_error()
            if err2:
                return err2

            return jsonify({"message": "Lobby lasciata. Partita annullata per mancanza giocatori.", "success": True}), 200

    return jsonify({"message": "Lobby lasciata con successo", "success": True}), 200


@app.route("/lobby/<int:lobby_id>/users", methods=["GET"])
def get_lobby_users(lobby_id):
    lobby = Lobby.query.get(lobby_id)
    if not lobby:
        return jsonify({"message": "Lobby non trovata"}), 404

    # Utenti attivi nella lobby specificata
    users = User.query.filter_by(lobby_id=lobby_id, banned=False).all()

    now = time.time()
    ACTIVE_SECONDS = 20  # Considera attivi gli utenti negli ultimi 20 secondi

    return jsonify([
        {
            "id": u.id,
            "username": u.username,
            "team": u.team,
            "lat": u.lat,
            "lon": u.lon,
            "avatar_seed": u.avatar_seed,
            "is_active": (now - u.last_active) < ACTIVE_SECONDS,
        }
        for u in users
    ]), 200


@app.route("/set_team", methods=["POST"])
def set_team():
    data = get_json()
    if not data:
        return jsonify({"success": False, "message": "JSON mancante o non valido"}), 400

    user_id = data.get("user_id")
    team = (data.get("team") or "").strip().upper()

    if not user_id or not team:
        return jsonify({"success": False, "message": "Dati mancanti"}), 400
    if team not in ALLOWED_TEAMS:
        return jsonify({"success": False, "message": "Team non valido (usa RED o BLUE)"}), 400

    user = User.query.get(user_id)
    if not user:
        return jsonify({"success": False, "message": "Utente non trovato"}), 404
    if user.banned:
        return jsonify({"success": False, "message": "Utente bannato"}), 403

    if user.lobby_id:
        lobby = Lobby.query.get(user.lobby_id)
        if not lobby:
            return jsonify({"success": False, "message": "Lobby non valida"}), 400

        # limite team (solo se sta entrando in quel team o cambiando)
        team_count = User.query.filter_by(lobby_id=user.lobby_id, team=team).count()
        if user.team != team and team_count >= LOBBY_TEAM_SIZE:
            return jsonify({"success": False, "message": f"Team {team} completo nella lobby"}), 409

        user.team = team
        err = db_commit_or_error()
        if err:
            return err

        red_count = User.query.filter_by(lobby_id=user.lobby_id, team="RED").count()
        blue_count = User.query.filter_by(lobby_id=user.lobby_id, team="BLUE").count()

        # avvio match: almeno 1 per team
        if lobby.status == "WAITING" and red_count >= 1 and blue_count >= 1:
            lobby.status = "ACTIVE"
            err2 = db_commit_or_error()
            if err2:
                return err2

            # genera target lobby e reset contatori
            err3 = generate_lobby_targets(lobby.id)
            if err3:
                return err3

        return jsonify({"success": True, "message": f"Team aggiornato a {user.team}"}), 200

    # fallback utenti senza lobby
    user.team = team
    err = db_commit_or_error()
    if err:
        return err
    return jsonify({"success": True, "message": f"Team aggiornato a {user.team}"}), 200


# ---------- TARGETS ----------

@app.route("/targets", methods=["GET"])
def get_targets():
    user_id = request.args.get("user_id", type=int)

    if user_id:
        user = User.query.get(user_id)
        if user and user.lobby_id:
            targets = Target.query.filter_by(lobby_id=user.lobby_id).all()
        else:
            targets = Target.query.filter(Target.lobby_id == None).all()
    else:
        targets = Target.query.filter(Target.lobby_id == None).all()

    return jsonify([
        {"id": t.id, "name": t.name, "lat": t.lat, "lon": t.lon, "owner": t.owner_team, "lobby_id": t.lobby_id}
        for t in targets
    ]), 200


@app.route("/hack", methods=["POST"])
def hack_target():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON non valido"}), 400

    user_id = data.get("user_id")
    target_id = data.get("target_id")

    if not user_id or not target_id:
        return jsonify({"message": "Dati mancanti (user_id, target_id)"}), 400

    user = User.query.get(user_id)
    target = Target.query.get(target_id)

    if not user or user.banned:
        return jsonify({"message": "Utente non valido"}), 403
    if not target:
        return jsonify({"message": "Target non trovato"}), 404
    if not user.team:
        return jsonify({"message": "Utente senza team"}), 400

    # target accessibile solo se globale o della stessa lobby
    if target.lobby_id is not None and target.lobby_id != user.lobby_id:
        return jsonify({"message": "Target non accessibile"}), 403

    # se target è di lobby, l'utente deve essere in lobby
    if target.lobby_id is not None and user.lobby_id is None:
        return jsonify({"message": "Devi essere in una lobby per hackare questo target"}), 403

    old_owner = target.owner_team
    new_owner = user.team

    # log sempre (anche se già tuo? a te la scelta: qui logghiamo solo se cambia)
    if old_owner == new_owner:
        return jsonify({"message": "Target già conquistato dal tuo team"}), 200

    log = HackLog(user_id=user.id, target_id=target.id, team=user.team)
    db.session.add(log)

    # aggiorna contatori SOLO per target di lobby (per i globali non hai contatori lobby)
    if target.lobby_id is not None:
        lobby = Lobby.query.get(target.lobby_id)
        lobby_adjust_target_counters(lobby, old_owner, new_owner)

    # aggiorna target
    target.owner_team = new_owner
    target.last_hacked = time.time()

    err = db_commit_or_error()
    if err:
        return err

    return jsonify({"message": "Hack registrato"}), 200


# ---------- LIVE POSITIONS ----------

@app.route("/update_position", methods=["POST"])
def update_position():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON non valido"}), 400

    user_id = data.get("user_id")
    lat = data.get("lat")
    lon = data.get("lon")

    if user_id is None or lat is None or lon is None:
        return jsonify({"message": "Dati mancanti"}), 400

    user = User.query.get(user_id)
    if not user or user.banned:
        return jsonify({"message": "Utente non valido"}), 403

    user.lat = float(lat)
    user.lon = float(lon)
    user.last_active = time.time()

    err = db_commit_or_error()
    if err:
        return err

    return jsonify({"success": True}), 200


@app.route("/users_positions", methods=["GET"])
def users_positions():
    now = time.time()
    ACTIVE_SECONDS = 10

    users = User.query.filter(
        User.last_active >= now - ACTIVE_SECONDS,
        User.banned == False
    ).all()

    return jsonify([
        {"id": u.id, "username": u.username, "lat": u.lat, "lon": u.lon, "team": u.team, "lobby_id": u.lobby_id}
        for u in users
    ]), 200


# ---------- ADMIN ----------

@app.route("/admin/users", methods=["GET"])
def get_all_users():
    users = User.query.all()
    return jsonify([
        {"id": u.id, "username": u.username, "admin": u.admin, "team": u.team, "score": u.score, "lobby_id": u.lobby_id}
        for u in users
    ]), 200


@app.route("/admin/ban_user/<int:user_id>", methods=["POST"])
def ban_user(user_id):
    user = User.query.get(user_id)
    if not user:
        return jsonify({"message": "Utente non trovato"}), 404

    user.banned = True
    err = db_commit_or_error()
    if err:
        return err
    return jsonify({"message": f"Utente {user.username} bannato"}), 200


@app.route("/admin/unban_user/<int:user_id>", methods=["POST"])
def unban_user(user_id):
    user = User.query.get(user_id)
    if not user:
        return jsonify({"message": "Utente non trovato"}), 404

    user.banned = False
    err = db_commit_or_error()
    if err:
        return err
    return jsonify({"message": f"Utente {user.username} sbannato"}), 200


@app.route("/admin/create_target", methods=["POST"])
def create_target():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON mancante"}), 400

    name = data.get("name")
    lat = data.get("lat")
    lon = data.get("lon")
    owner = (data.get("owner_team") or "NEUTRAL").strip().upper()

    if not name or lat is None or lon is None:
        return jsonify({"message": "Dati target incompleti"}), 400

    if owner not in {'NEUTRAL', 'RED', 'BLUE'}:
        owner = "NEUTRAL"

    new_target = Target(name=name, lat=float(lat), lon=float(lon), owner_team=owner, lobby_id=None)
    db.session.add(new_target)

    err = db_commit_or_error()
    if err:
        return err

    return jsonify({"message": "Target creato"}), 200


@app.route("/admin/delete_target/<int:target_id>", methods=["DELETE"])
def delete_target(target_id):
    target = Target.query.get(target_id)
    if not target:
        return jsonify({"message": "Target non trovato"}), 404

    db.session.delete(target)
    err = db_commit_or_error()
    if err:
        return err

    return jsonify({"message": "Target eliminato"}), 200


# ---------- DEBUG: RANDOM TARGETS (GLOBAL) ----------

@app.route("/generate_random_targets", methods=["POST"])
def generate_random_targets():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON non valido"}), 400

    lat = data.get("lat")
    lon = data.get("lon")
    count = int(data.get("count", 10))
    radius_km = float(data.get("radius_km", 30))

    if lat is None or lon is None:
        return jsonify({"message": "Dati mancanti (lat, lon)"}), 400

    R = 6378.1  # km

    new_targets = []
    for i in range(count):
        radius_rad = radius_km / R
        r = radius_rad * math.sqrt(random.random())
        theta = 2 * math.pi * random.random()
        lat_rad = math.radians(float(lat))
        lon_rad = math.radians(float(lon))

        new_lat_rad = math.asin(
            math.sin(lat_rad) * math.cos(r) +
            math.cos(lat_rad) * math.sin(r) * math.cos(theta)
        )
        new_lon_rad = lon_rad + math.atan2(
            math.sin(theta) * math.sin(r) * math.cos(lat_rad),
            math.cos(r) - math.sin(lat_rad) * math.sin(new_lat_rad)
        )

        new_lat = math.degrees(new_lat_rad)
        new_lon = math.degrees(new_lon_rad)

        target_name = f"Obiettivo Casuale #{int(time.time()) % 1000 + i}"

        new_targets.append(Target(
            name=target_name,
            lat=new_lat,
            lon=new_lon,
            owner_team="NEUTRAL",
            lobby_id=None
        ))

    db.session.add_all(new_targets)
    err = db_commit_or_error()
    if err:
        return err

    return jsonify({"message": f"{count} target casuali generati con successo"}), 200


# -------------- LIVE APP -----------------

if __name__ == "__main__":
    app.run(port=5500)
