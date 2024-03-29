#!/usr/bin/python3
import subprocess
import lib.azure as azure
import argparse


def main():
    args = argparse.ArgumentParser()
    args.add_argument("function_names", nargs="*")
    args = args.parse_args()

    azure_env = {
        s[0]: s[1]
        for s in map(lambda l: l.split("=", 1), open("azure.env").read().splitlines())
    }

    functions = list(
        [azure.function_artifact_id(fname) for fname in azure.list_function_names()]
        if len(args.function_names) == 0
        else args.function_names
    )
    for appname in functions + ["scc-backend-d464", "scc-backend-d464-centralus"]:
        subprocess.run(
            [
                "az",
                "functionapp",
                "config",
                "appsettings",
                "set",
                "--resource-group",
                azure.RESOURCE_GROUP,
                "--name",
                appname,
                "--settings",
            ]
            + [f"{k}={v}" for k, v in azure_env.items()],
        )


if __name__ == "__main__":
    main()
