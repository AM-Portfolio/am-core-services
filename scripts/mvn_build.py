import sys
import os
import xml.etree.ElementTree as ET
import subprocess

def main():
    if len(sys.argv) < 2:
        print("Usage: python scripts/mvn_build.py <directory_prefix>")
        sys.exit(1)

    target_prefix = sys.argv[1]
    
    # Parse pom.xml to find modules registered in the reactor
    try:
        # Resolve path to pom.xml (root of the repo)
        pom_path = 'pom.xml'
        if not os.path.exists(pom_path):
            print(f"Error: pom.xml not found at {os.getcwd()}")
            sys.exit(1)

        tree = ET.parse(pom_path)
        root = tree.getroot()
        
        # Maven POMs usually have a namespace
        ns_url = "http://maven.apache.org/POM/4.0.0"
        ns = {'mvn': ns_url}
        
        # Find all modules
        modules = []
        # Support both with and without namespace for robustness
        find_query = './/mvn:module' if ns_url in root.tag else './/module'
        
        for module in root.findall(find_query, ns):
            module_path = module.text.strip()
            # Check if module path starts with the requested prefix (e.g., 'libraries/')
            if module_path.startswith(target_prefix):
                modules.append(module_path)
        
        if not modules:
            print(f"No modules found matching prefix: {target_prefix}")
            sys.exit(1)
            
        # Construct comma-separated project list
        pl_arg = ",".join(modules)
        
        # Build command list
        command = [
            "mvn", "clean", "install", 
            "-pl", pl_arg, 
            "-am", 
            "-s", "maven-settings.xml", 
            "-DskipTests"
        ]
        
        print(f"--- AM Build Orchestrator ---")
        print(f"Target Prefix: {target_prefix}")
        print(f"Found Modules: {len(modules)}")
        print(f"Executing: {' '.join(command)}")
        print(f"----------------------------")
        
        # Use shell=True on Windows for better mvn command discovery if needed, 
        # but list format is usually safer.
        result = subprocess.run(command, shell=(os.name == 'nt'))
        sys.exit(result.returncode)
        
    except Exception as e:
        print(f"Fatal Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
