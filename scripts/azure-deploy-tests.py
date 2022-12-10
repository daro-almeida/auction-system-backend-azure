import subprocess
import lib.azure as azure

if __name__ == '__main__':
    subprocess.run(
        ["az", "container", "create",
         "--resource-group", azure.RESOURCE_GROUP,
         "--name", "scc-tester",
         "--restart-policy", "Never",
         "--image", "git.d464.sh/diogo464/scc-tester:latest",
         "--environment-variables", "TESTID=azure", "TARGET="+azure.TARGET])
