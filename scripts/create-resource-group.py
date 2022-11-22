#!/usr/bin/python

import subprocess
import lib.azure as azure

subprocess.run(
    [
        "az",
        "group",
        "create",
        "--name",
        azure.RESOURCE_GROUP,
        "--location",
        azure.REGION,
    ]
)
