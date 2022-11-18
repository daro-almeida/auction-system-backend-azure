#!/usr/bin/python3
import os
import sys
import subprocess
import argparse
import lib.azure as azure


def function_class_name(name: str) -> str:
    return "".join(map(lambda s: s.capitalize(), name.split("-")))


def find_all_jarfiles() -> list[str]:
    blacklisted_fragments = ["slf4j-jdk14", "slf4j-log4j12"]
    jars = []
    for root, _, files in os.walk(f"{os.environ['HOME']}/.m2"):
        for file in files:
            if file.endswith(".jar"):
                jars.append(f"{root}/{file}")
    return list(
        filter(
            lambda jar: not any(
                blacklisted_fragment in jar
                for blacklisted_fragment in blacklisted_fragments
            ),
            jars,
        )
    )


def main():
    args = argparse.ArgumentParser()
    args.add_argument("function_name")
    args = args.parse_args()

    classpath = (
        ":".join(find_all_jarfiles())
        + f":modules/{azure.function_directory_name(sys.argv[1])}/target/classes"
        + f":modules/backend/target/classes"
        + f":modules/backend-azure/target/classes"
    )

    try:
        subprocess.run(
            [
                "java",
                "-cp",
                classpath,
                f"scc.azure.functions.{function_class_name(sys.argv[1])}",
            ]
            + sys.argv[2:],
        ).check_returncode()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
