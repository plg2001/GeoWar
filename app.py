import os
import time

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

# ---------------- MODELS ----------------

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

    avatar_seed = db.Column(db.String(80))
    admin = db.Column(db.Boolean, default=False)
    banned = db.Column(db.Boolean, default=False)


class Target(db.Model):
    __tablename__ = "target"

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(80), nullable=False)
    lat = db.Column(db.Float, nullable=False)
    lon = db.Column(db.Float, nullable=False)

    owner_team = db.Column(db.String(10), default="NEUTRAL")
    last_hacked = db.Column(db.Float, default=0.0)


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
    # Inizializzazione target di esempio se non esistono
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
                "admin": user.admin
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
        "avatar_seed": user.avatar_seed
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

    # TODO: Aggiungere controllo che l'utente che fa la richiesta sia l'utente stesso o un admin

    new_username = data.get("username", "").strip()
    new_email = data.get("email", "").strip().lower()

    # Validazione
    if not new_username or not new_email:
        return jsonify({"message": "Username e email non possono essere vuoti"}), 400

    # Controlla se il nuovo username o email sono già in uso da *altri* utenti
    if User.query.filter(User.id != user_id, User.username == new_username).first():
        return jsonify({"message": "Username già in uso"}), 409

    if User.query.filter(User.id != user_id, User.email == new_email).first():
        return jsonify({"message": "Email già in uso"}), 409

    user.username = new_username
    user.email = new_email

    # Gestione avatar_seed
    new_avatar_seed = data.get("avatar_seed")
    if new_avatar_seed:
        user.avatar_seed = new_avatar_seed.strip()

    err = db_commit_or_500()
    if err:
        return err

    return jsonify({"message": "Profilo aggiornato con successo"}), 200


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

    user.team = team
    err = db_commit_or_500()
    if err:
        return err

    return jsonify({"success": True, "message": f"Team aggiornato a {user.team}"}), 200


@app.route("/targets", methods=["GET"])
def get_targets():
    targets = Target.query.all()
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
                    "admin": user.admin
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
            # hash di una stringa non utilizzabile; evita check_password_hash crash
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
                "admin": new_user.admin
            }
        }), 200

    except ValueError:
        return jsonify({"message": "Token Google non valido"}), 401
    except Exception as e:
        # rollback di sicurezza se qualcosa ha toccato la sessione
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
        "score": u.score
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

    users = User.query.filter(
        User.last_active >= now - ACTIVE_SECONDS,
        User.banned == False
    ).all()

    return jsonify([{
        "id": u.id,
        "username": u.username,
        "lat": u.lat,
        "lon": u.lon,
        "team": u.team
    } for u in users]), 200


if __name__ == "__main__":
    app.run(port=5500)
