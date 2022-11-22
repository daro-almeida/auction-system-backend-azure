#!/usr/bin/python

import subprocess
import lib.azure as azure

subprocess.run(
    [
        "az",
        "container",
        "restart",
        "--resource-group",
        azure.RESOURCE_GROUP,
        "--name",
        "scc-backend",
    ]
)
