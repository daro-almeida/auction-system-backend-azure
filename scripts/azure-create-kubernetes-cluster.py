#!/usr/bin/python

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
            "scc",
            "--node-vm-size",
            "Standard_B2s",
            "--node-count",
            "2",
            "--enable-addons",
            "http_application_routing",
            "--generate-ssh-keys",
        ]
    ).check_returncode()

    subprocess.run(
        [
            "az",
            "aks",
            "get-credentials",
            "--resource-group",
            azure.RESOURCE_GROUP,
            "--name",
            "scc",
        ]
    ).check_returncode()

    subprocess.run(["kubectl", "config", "use-context", "scc"]).check_returncode()

    subprocess.run(["kubectl", "apply", "-f", "deploy/azure"]).check_returncode()
