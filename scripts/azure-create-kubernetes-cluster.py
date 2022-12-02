import subprocess
import lib.azure as azure


if __name__ == "__main__":
    subprocess.run(
        [
            "az",
            "aks",
            "create",
            "--resource-group",
            azure.RESOURCE_GROUP,
            "--name",
            "scc-backend-app",
            "--node-vm-size",
            "Standard_B2s",
            "--node-count",
            "4",
            "--generate-ssh-keys",
            "--service-principal",
            "64c1c360-5bbd-45a8-8d11-543b18b4a2bc",
            "--client-secret",
            "_pa8Q~-tdl_wDO8KWzWLXicme1tlCCr2D9UtRcoK",
        ]
    )

    subprocess.run(["kubectl", "apply", "-f", "../deploy/backend.yaml"])
    subprocess.run(["kubectl", "apply", "-f", "../deploy/mongo.yaml"])
    subprocess.run(["kubectl", "apply", "-f", "../deploy/redis.yaml"])
    subprocess.run(["kubectl", "apply", "-f", "../deploy/rabbitmq.yaml"])

    subprocess.run(["kubectl", "get", "services"])
    subprocess.run(["kubectl", "get", "pods"])
