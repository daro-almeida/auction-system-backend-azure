#!/usr/bin/python

import subprocess
import lib.azure as azure

print("Dont forget to run mvn compile package")
for func in azure.list_function_names():
    print(f"Deploying function {func}")
    module = f"modules/{azure.function_directory_name(func)}"
    subprocess.run(["mvn", "azure-functions:deploy", "-pl", module])
