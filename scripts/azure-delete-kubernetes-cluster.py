import subprocess
import lib.azure as azure


if __name__ == "__main__":
    subprocess.run(["kubectl", "delete", "-f", "deploy/azure"]).check_returncode()
    subprocess.run(["az", "group", "delete", "--resource-group", azure.RESOURCE_GROUP])
