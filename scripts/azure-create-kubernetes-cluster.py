import subprocess
import lib.azure as azure


def create_app():
    subprocess.run(["az", "aks", "create", "--resource-group", azure.RESOURCE_GROUP, "--name", "scc-backend-app",
                    "--node-vm-size", "standard_b2s", "--generate-ssh-keys"])


if __name__ == '__main__':
    delete = False
    # delete = True
    create_app()

