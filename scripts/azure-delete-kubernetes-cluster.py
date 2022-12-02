import subprocess
import lib.azure as azure


if __name__ == "__main__":
    subprocess.run(["kubectl", "delete", "deployments,services,pods", "--all"])
    subprocess.run(["kubectl", "delete", "pv", "--all"])
    subprocess.run(["az", "group", "delete", "--resource-group", azure.RESOURCE_GROUP])
