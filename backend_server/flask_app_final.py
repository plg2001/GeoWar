from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
import math
import time

# Inizializza Flask
app = Flask(__name__)

# Configurazione DB
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///geowar.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

# ---------------- MODELLI DB ----------------

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False) # NUOVO CAMPO
    password = db.Column(db.String(120), nullable=False)
    team = db.Column(db.String(10), default=None)
    score = db.Column(db.Integer, default=0)
    lat = db.Column(db.Float, default=0.0)
    lon = db.Column(db.Float, default=0.0)
    last_active = db.Column(db.Float, default=0.0)

class Target(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(80), nullable=False)
    lat = db.Column(db.Float, nullable=False)
    lon = db.Column(db.Float, nullable=False)
    owner_team = db.Column(db.String(10), default='NEUTRAL')

# Crea le tabelle se non esistono
with app.app_context():
    db.create_all()

# ---------------- ENDPOINTS API ----------------

@app.route('/')
def home():
    return jsonify({"status": "API online", "message": "GeoWar Server Ready"})

@app.route('/register', methods=['POST'])
def register():
    data = request.json
    # Controllo campi obbligatori
    if not data or 'username' not in data or 'password' not in data or 'email' not in data:
        return jsonify({"message": "Dati mancanti (username, password, email)"}), 400

    if User.query.filter_by(username=data['username']).first():
        return jsonify({"message": "Username gia esistente"}), 400
    
    if User.query.filter_by(email=data['email']).first():
        return jsonify({"message": "Email gia registrata"}), 400
    
    new_user = User(
        username=data['username'],
        email=data['email'],
        password=data['password'] # In produzione usare hashing!
    )
    db.session.add(new_user)
    db.session.commit()
    
    return jsonify({
        "message": "Registrazione completata",
        "user": {
            "id": new_user.id,
            "username": new_user.username
        }
    }), 200

@app.route('/login', methods=['POST'])
def login():
    data = request.json
    user = User.query.filter_by(username=data['username'], password=data['password']).first()
    
    if user:
        return jsonify({
            "message": "Login effettuato",
            "user": {
                "id": user.id,
                "username": user.username
            },
            "token": "fake-jwt-token-12345" 
        }), 200
        
    return jsonify({"message": "Credenziali errate"}), 401

@app.route('/targets', methods=['GET'])
def get_targets():
    targets = Target.query.all()
    return jsonify([{
        "id": t.id,
        "name": t.name,
        "lat": t.lat,
        "lon": t.lon,
        "owner": t.owner_team
    } for t in targets])

if __name__ == '__main__':
    app.run(port=5000)
