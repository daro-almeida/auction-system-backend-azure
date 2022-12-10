import subprocess
import lib.azure as azure

if __name__ == '__main__':
    subprocess.run(
        ["az", "container", "delete",
         "--resource-group", azure.RESOURCE_GROUP,
         "--name", "scc-tester"])
