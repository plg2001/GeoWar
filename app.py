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

# Consiglio: metti la URI in variabile d'ambiente (più sicuro).
# Se vuoi tenere hardcoded, lascia pure questa stringa.
DB_URI = os.getenv(
    "DATABASE_URL",
    "mysql+pymysql://Plg2001:mydatabase@Plg2001.mysql.pythonanywhere-services.com/Plg2001$default?charset=utf8mb4",
)

app.config["SQLALCHEMY_DATABASE_URI"] = DB_URI
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

# Evita "MySQL server has gone away" su hosting/idle
app.config["SQLALCHEMY_ENGINE_OPTIONS"] = {
    "pool_pre_ping": True,
    "pool_recycle": 280,
}

db = SQLAlchemy(app)

ALLOWED_TEAMS = {"RED", "BLUE"}

# MODIFICA RICHIESTA: Max 20 totali (10 per team)
LOBBY_MAX_PLAYERS = 20
LOBBY_TEAM_SIZE = 10

# ---------------- MODELS ----------------

class Lobby(db.Model):
    __tablename__ = "lobby"
    id = db.Column(db.Integer, primary_key=True)
    status = db.Column(db.String(20), default="WAITING") # WAITING, ACTIVE, FINISHED
    created_at = db.Column(db.Float, default=time.time)

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

    owner_team = db.Column(db.String(10), default="NEUTRAL")
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


with app.app_context():
    db.create_all()
    # Inizializzazione target di esempio se non esistono (Globali / Lobby NULL)
    if not Target.query.first():
        sample_targets = [
            Target(name="Duomo di Milano", lat=45.4641, lon=9.1919, owner_team="NEUTRAL"),
            Target(name="Castello Sforzesco", lat=45.4705, lon=9.1793, owner_team="NEUTRAL"),
            Target(name="Stazione Centrale", lat=45.4859, lon=9.2035, owner_team="NEUTRAL"),
            Target(name="Arco della Pace", lat=45.4754, lon=9.1724, owner_team="NEUTRAL"),
            Target(name="Politecnico di Milano", lat=45.4790, lon=9.2274, owner_team="NEUTRAL")
        ]
        db.session.add_all(sample_targets)
        db.session.commit()

# ---------------- HELPERS ----------------

def get_json():
    data = request.get_json(silent=True)
    return data if isinstance(data, dict) else None

def db_commit_or_500():
    try:
        db.session.commit()
        return None
    except IntegrityError as e:
        db.session.rollback()
        return jsonify({"message": "Errore di integrità database", "detail": str(e)}), 409
    except SQLAlchemyError as e:
        db.session.rollback()
        return jsonify({"message": "Errore database", "detail": str(e)}), 500

def generate_lobby_targets(lobby_id, count=20):
    # Genera target randomici per l'Italia per una specifica lobby
    # Bounding box approssimativo Italia
    min_lat, max_lat = 36.6, 47.1
    min_lon, max_lon = 6.6, 18.5
    
    new_targets = []
    for i in range(count):
        lat = random.uniform(min_lat, max_lat)
        lon = random.uniform(min_lon, max_lon)
        
        target = Target(
            name=f"Obiettivo Lobby {lobby_id} #{i+1}",
            lat=lat,
            lon=lon,
            owner_team="NEUTRAL",
            lobby_id=lobby_id
        )
        new_targets.append(target)
    
    db.session.add_all(new_targets)
    db.session.commit()

# ---------------- ROUTES ----------------

@app.route("/")
def home():
    return jsonify({"status": "API online", "message": "GeoWar Server Ready"}), 200


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
    err = db_commit_or_500()
    if err:
        return err

    return jsonify({
        "message": "Registrazione completata",
        "user": {"id": new_user.id, "username": new_user.username}
    }), 200


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

    if check_password_hash(user.password_hash, password):
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

    return jsonify({"message": "Credenziali errate"}), 401


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
        "avatar_seed": user.avatar_seed,
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

    new_username = data.get("username", "").strip()
    new_email = data.get("email", "").strip().lower()

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

    err = db_commit_or_500()
    if err:
        return err

    return jsonify({"message": "Profilo aggiornato con successo"}), 200


@app.route("/lobby/join", methods=["POST"])
def join_lobby():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON mancante"}), 400
        
    user_id = data.get("user_id")
    if not user_id:
        return jsonify({"message": "User ID mancante"}), 400
        
    user = User.query.get(user_id)
    if not user or user.banned:
        return jsonify({"message": "Utente non valido"}), 403

    # Se l'utente è già in una lobby WAITING, rimaniamoci
    if user.lobby_id:
        current_lobby = Lobby.query.get(user.lobby_id)
        if current_lobby and current_lobby.status == "WAITING":
            return jsonify({
                "message": "Già in una lobby",
                "lobby_id": str(current_lobby.id),
                "team": user.team
            }), 200
            
    # Cerca una lobby WAITING con meno di 20 giocatori
    lobbies = Lobby.query.filter_by(status="WAITING").all()
    target_lobby = None
    
    for lobby in lobbies:
        count = User.query.filter_by(lobby_id=lobby.id).count()
        if count < LOBBY_MAX_PLAYERS:
            target_lobby = lobby
            break
            
    if not target_lobby:
        target_lobby = Lobby(status="WAITING")
        db.session.add(target_lobby)
        db.session.commit()
        
    user.lobby_id = target_lobby.id
    user.team = None # Reset team when joining new lobby
    db_commit_or_500()
    
    return jsonify({
        "message": "Lobby assegnata",
        "lobby_id": str(target_lobby.id)
    }), 200


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

    # Controllo Lobby
    if user.lobby_id:
        lobby = Lobby.query.get(user.lobby_id)
        if lobby:
            # Conta giocatori nel team
            team_count = User.query.filter_by(lobby_id=user.lobby_id, team=team).count()
            if team_count >= LOBBY_TEAM_SIZE:
                return jsonify({"success": False, "message": f"Team {team} completo nella lobby"}), 409
            
            user.team = team
            db.session.commit()
            
            # Conta i giocatori per squadra
            red_count = User.query.filter_by(lobby_id=user.lobby_id, team="RED").count()
            blue_count = User.query.filter_by(lobby_id=user.lobby_id, team="BLUE").count()
            
            # MODIFICA RICHIESTA: Avvia se c'è almeno 1 giocatore per team
            if lobby.status == "WAITING" and red_count >= 1 and blue_count >= 1:
                lobby.status = "ACTIVE"
                db.session.commit()
                generate_lobby_targets(lobby.id)
                
            return jsonify({"success": True, "message": f"Team aggiornato a {user.team}"}), 200

    # Fallback per utenti senza lobby (compatibilità)
    user.team = team
    err = db_commit_or_500()
    if err:
        return err

    return jsonify({"success": True, "message": f"Team aggiornato a {user.team}"}), 200


@app.route("/targets", methods=["GET"])
def get_targets():
    user_id = request.args.get('user_id', type=int)
    
    if user_id:
        user = User.query.get(user_id)
        if user and user.lobby_id:
            # Ritorna target della lobby dell'utente
            targets = Target.query.filter_by(lobby_id=user.lobby_id).all()
        else:
             # Utente senza lobby o ID non trovato -> Targets globali (lobby_id NULL)
             targets = Target.query.filter(Target.lobby_id == None).all()
    else:
        # Nessun user_id specificato -> comportamento legacy o admin, ritorna TUTTI o solo globali?
        # Per admin meglio vedere tutto, ma per ora torniamo i globali per sicurezza
        targets = Target.query.filter(Target.lobby_id == None).all()

    return jsonify([{
        "id": t.id,
        "name": t.name,
        "lat": t.lat,
        "lon": t.lon,
        "owner": t.owner_team
    } for t in targets]), 200


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
        err = db_commit_or_500()
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


# --- ADMIN ENDPOINTS ---

@app.route("/admin/users", methods=["GET"])
def get_all_users():
    users = User.query.all()
    return jsonify([{
        "id": u.id,
        "username": u.username,
        "admin": u.admin,
        "team": u.team,
        "score": u.score,
        "lobby_id": u.lobby_id
    } for u in users]), 200

@app.route("/admin/ban_user/<int:user_id>", methods=["POST"])
def ban_user(user_id):
    user = User.query.get(user_id)
    if not user:
        return jsonify({"message": "Utente non trovato"}), 404

    user.banned = True
    db.session.commit()
    return jsonify({"message": f"Utente {user.username} bannato"}), 200

@app.route("/admin/unban_user/<int:user_id>", methods=["POST"])
def unban_user(user_id):
    user = User.query.get(user_id)
    if not user:
        return jsonify({"message": "Utente non trovato"}), 404

    user.banned = False
    db.session.commit()
    return jsonify({"message": f"Utente {user.username} sbannato"}), 200

@app.route("/admin/create_target", methods=["POST"])
def create_target():
    data = get_json()
    if not data:
        return jsonify({"message": "JSON mancante"}), 400

    name = data.get("name")
    lat = data.get("lat")
    lon = data.get("lon")
    owner = data.get("owner_team", "NEUTRAL")

    if not name or lat is None or lon is None:
        return jsonify({"message": "Dati target incompleti"}), 400

    new_target = Target(name=name, lat=lat, lon=lon, owner_team=owner)
    # Target creato da admin è globale (lobby_id = None)
    db.session.add(new_target)
    err = db_commit_or_500()
    if err:
        return err

    return jsonify({"message": "Target creato"}), 200

@app.route("/admin/delete_target/<int:target_id>", methods=["DELETE"])
def delete_target(target_id):
    target = Target.query.get(target_id)
    if not target:
        return jsonify({"message": "Target non trovato"}), 404

    db.session.delete(target)
    err = db_commit_or_500()
    if err:
        return err

    return jsonify({"message": "Target eliminato"}), 200



# -------------- LIVE APP -----------------

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

    err = db_commit_or_500()
    if err:
        return err

    return jsonify({"success": True}), 200



@app.route("/users_positions", methods=["GET"])
def users_positions():
    now = time.time()
    ACTIVE_SECONDS = 10

    # Return only users active recently. 
    # TODO: In future, filter by lobby? For now show all active.
    users = User.query.filter(
        User.last_active >= now - ACTIVE_SECONDS,
        User.banned == False
    ).all()

    return jsonify([{
        "id": u.id,
        "username": u.username,
        "lat": u.lat,
        "lon": u.lon,
        "team": u.team,
        "lobby_id": u.lobby_id
    } for u in users]), 200


@app.route("/generate_random_targets", methods=["POST"])
def generate_random_targets():
    # Per debug o admin, genera target globali o locali se specificato lobby
    data = get_json()
    if not data:
        return jsonify({"message": "JSON non valido"}), 400

    lat = data.get("lat")
    lon = data.get("lon")
    count = data.get("count", 10)
    radius_km = data.get("radius_km", 30)

    if lat is None or lon is None:
        return jsonify({"message": "Dati mancanti (lat, lon)"}), 400

    # Earth radius in kilometers
    R = 6378.1 

    new_targets = []
    for i in range(count):
        radius_rad = radius_km / R
        r = radius_rad * math.sqrt(random.random())
        theta = 2 * math.pi * random.random()
        lat_rad = math.radians(lat)
        lon_rad = math.radians(lon)
        new_lat_rad = math.asin(math.sin(lat_rad) * math.cos(r) + math.cos(lat_rad) * math.sin(r) * math.cos(theta))
        new_lon_rad = lon_rad + math.atan2(math.sin(theta) * math.sin(r) * math.cos(lat_rad), math.cos(r) - math.sin(lat_rad) * math.sin(new_lat_rad))
        new_lat = math.degrees(new_lat_rad)
        new_lon = math.degrees(new_lon_rad)

        target_name = f"Obiettivo Casuale #{int(time.time()) % 1000 + i}"

        target = Target(
            name=target_name,
            lat=new_lat,
            lon=new_lon,
            owner_team="NEUTRAL"
        )
        new_targets.append(target)
    
    db.session.add_all(new_targets)
    err = db_commit_or_500()
    if err:
        return err

    return jsonify({"message": f"{count} target casuali generati con successo"}), 200


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
        
    # Check if target belongs to user's lobby or is global
    if target.lobby_id and target.lobby_id != user.lobby_id:
         return jsonify({"message": "Target non accessibile"}), 403

    log = HackLog(
        user_id=user.id,
        target_id=target.id,
        team=user.team
    )

    target.owner_team = user.team
    target.last_hacked = time.time()

    db.session.add(log)
    err = db_commit_or_500()
    if err:
        return err

    return jsonify({"message": "Hack registrato"}), 200


if __name__ == "__main__":
    app.run(port=5500)
