#!/usr/bin/python

import subprocess
import lib.azure as azure

subprocess.run(
    [
        "az",
        "container",
        "create",
        "--resource-group",
        azure.RESOURCE_GROUP,
        "--name",
        "scc-backend",
        "--image",
        "ghcr.io/diogo464/scc-backend-app",
        "--ports",
        "8080",
        "--dns-name-label",
        "scc-backend",
    ]
)
