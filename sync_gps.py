import requests
import os
import sys
import subprocess
import platform

def find_adb():
    """Cerca l'eseguibile ADB nel sistema."""
    # 1. Prova a vedere se 'adb' è già nel PATH
    try:
        subprocess.run(["adb", "version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        return "adb"
    except (FileNotFoundError, subprocess.CalledProcessError):
        pass

    # 2. Se siamo su Windows, cerca nel percorso standard
    if platform.system() == "Windows":
        local_app_data = os.environ.get('LOCALAPPDATA')
        if local_app_data:
            adb_path = os.path.join(local_app_data, "Android", "Sdk", "platform-tools", "adb.exe")
            if os.path.exists(adb_path):
                return f'"{adb_path}"' # Aggiungi virgolette per gestire spazi

    # 3. Fallback: chiedi all'utente
    return None

def sync_gps():
    print("🔍 Cerco ADB...")
    adb_command = find_adb()
    
    if not adb_command:
        print("❌ Errore: Non riesco a trovare 'adb.exe'.")
        print("Assicurati di aver installato Android SDK Platform-Tools.")
        return

    print(f"✅ ADB trovato: {adb_command}")
    print("🌍 Recupero la posizione del PC...")
    
    try:
        # 1. Ottieni le coordinate del PC tramite IP
        response = requests.get("http://ip-api.com/json/")
        data = response.json()
        
        if data['status'] == 'fail':
            print("❌ Errore nel recupero IP")
            return

        lat = data['lat']
        lon = data['lon']
        city = data['city']
        
        print(f"📍 Posizione trovata: {city} ({lat}, {lon})")
        
        # 2. Invia le coordinate all'emulatore
        # Nota: geo fix vuole <longitude> <latitude>
        cmd = f"{adb_command} emu geo fix {lon} {lat}"
        
        print(f"📲 Invio all'emulatore...")
        result = os.system(cmd)
        
        if result == 0:
            print("✅ Posizione aggiornata con successo nell'emulatore!")
        else:
            print("⚠️ Errore durante l'invio del comando. Assicurati che l'emulatore sia APERTO.")

    except ImportError:
        print("❌ Errore: Manca la libreria 'requests'.")
        print("Esegui: pip install requests")
    except Exception as e:
        print(f"Errore imprevisto: {e}")

if __name__ == "__main__":
    sync_gps()
    print("\nPremi Invio per chiudere...")
    input()
