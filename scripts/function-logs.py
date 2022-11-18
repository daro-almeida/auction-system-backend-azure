#!/usr/bin/python3
import sys
import subprocess
import lib.azure as azure
import argparse


def main():
    args = argparse.ArgumentParser()
    args.add_argument("function_name")
    args = args.parse_args()

    try:
        subprocess.run(
            [
                "az",
                "webapp",
                "log",
                "tail",
                "--resource-group",
                azure.RESOURCE_GROUP,
                "--name",
                azure.function_artifact_id(args.function_name),
            ]
        )
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
