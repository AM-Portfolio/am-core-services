import os
import re
import csv
import json
import ssl
import urllib.request
from datetime import datetime

# --- Configuration ---
VAULT_ADDR = os.getenv('VAULT_ADDR', 'http://127.0.0.1:8200')
VAULT_TOKEN = os.getenv('VAULT_TOKEN', '')
MANIFEST_FILE = 'vault_requirements.csv'
BACKUP_DIR = 'vault_backups'

# Regex to find Spring Boot placeholders: ${VAR_NAME:default_value}
VAR_PATTERN = re.compile(r'\${([A-Z0-9_]+)(?::([^}]*))?}')

def scan_codebase():
    """Scans the codebase for all required environment variables."""
    variables = {}
    search_dirs = ['services']
    
    print(f"[*] Scanning codebase for environment variables...")
    for s_dir in search_dirs:
        for root, _, files in os.walk(s_dir):
            for file in files:
                if file.endswith(('.yml', '.yaml')):
                    path = os.path.join(root, file)
                    with open(path, 'r') as f:
                        content = f.read()
                        matches = VAR_PATTERN.findall(content)
                        for var_name, default_val in matches:
                            variables[var_name] = default_val
    return variables

def generate_manifest(variables):
    """Generates the initial CSV manifest with placeholder values."""
    if os.path.exists(MANIFEST_FILE):
        print(f"[!] {MANIFEST_FILE} already exists. Skipping generation.")
        return

    with open(MANIFEST_FILE, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['Variable', 'Current_Value_In_Vault', 'New_Value', 'Default_Found', 'Vault_Path'])
        for var, default in sorted(variables.items()):
            # Assign standard paths based on variable prefix or common usage
            path = "kv/prod/app"
            if "KAFKA" in var: path = "kv/prod/kafka"
            if "REDIS" in var: path = "kv/prod/database/redis"
            if "MONGO" in var: path = "kv/prod/database/mongodb"
            
            writer.writerow([var, '', '[OVERWRITE_THIS]', default, path])
    print(f"[+] Generated {MANIFEST_FILE}. Please update it with real values.")

def vault_api(method, path, data=None):
    """Simple wrapper for Vault API calls using urllib."""
    url = f"{VAULT_ADDR.rstrip('/')}/v1/{path.lstrip('/')}"
    headers = {
        'X-Vault-Token': VAULT_TOKEN,
        'Content-Type': 'application/json'
    }
    
    req = urllib.request.Request(url, headers=headers, method=method)
    if data:
        req.data = json.dumps(data).encode('utf-8')
    
    try:
        # Create unverified context for self-signed certs (common in internal Vaults)
        context = ssl._create_unverified_context()
        with urllib.request.urlopen(req, context=context) as response:
            res_body = response.read().decode('utf-8')
            return json.loads(res_body) if res_body else {}
    except Exception as e:
        print(f"[ERROR] Vault API {method} {path} failed: {e}")
        return None

def sync_to_vault():
    """Reads the CSV and pushes missing/updated values to Vault."""
    if not VAULT_TOKEN:
        print("[ERROR] VAULT_TOKEN environment variable is not set.")
        return

    print(f"[*] Starting Vault synchronization...")
    
    # 1. Backup current state
    if not os.path.exists(BACKUP_DIR):
        os.makedirs(BACKUP_DIR)
    
    with open(MANIFEST_FILE, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            path = row['Vault_Path']
            var = row['Variable']
            val = row['New_Value']
            
            if val == '[OVERWRITE_THIS]' or not val:
                print(f"[SKIP] {var} - No value provided in CSV.")
                continue

            # Fetch existing to avoid overwrite
            # Note: Assuming KV Version 2 (data/ prefix)
            full_path = f"{path.split('/')[0]}/data/{'/'.join(path.split('/')[1:])}"
            current = vault_api('GET', full_path)
            
            payload = {}
            if current and 'data' in current and 'data' in current['data']:
                payload = current['data']['data']
                if var in payload:
                    print(f"[KEEP] {var} - Already exists in Vault. Use '--force' to overwrite (not implemented).")
                    continue
            
            # Add new value
            payload[var] = val
            print(f"[*] Writing {var} to {path}...")
            vault_api('POST', full_path, {'data': payload})

if __name__ == "__main__":
    import sys
    detected_vars = scan_codebase()
    generate_manifest(detected_vars)
    
    if len(sys.argv) > 1 and sys.argv[1] == '--sync':
        sync_to_vault()
    else:
        print("\n[TIP] Update 'vault_requirements.csv' then run this script with '--sync' to push to Vault.")
        print("[TIP] Make sure to export VAULT_ADDR and VAULT_TOKEN first.")
