#!/usr/bin/python3
import os
import re
import subprocess
import argparse
import xml.etree.ElementTree as ET
import lib.azure as azure


def assert_worker_name_is_valid(function_name):
    matches = re.match(r"^[a-z][a-z0-9-]*$", function_name)
    if not matches:
        raise ValueError(
            "Function name must be lowercase, alphanumeric and may contain hyphens"
        )


def main():
    args = argparse.ArgumentParser()
    args.add_argument("worker_name")
    args = args.parse_args()

    assert_worker_name_is_valid(args.worker_name)
    ET.register_namespace("", "http://maven.apache.org/POM/4.0.0")

    artifact_id = f"scc-worker-{args.worker_name}"
    worker_name = args.worker_name
    worker_dir_name = f"worker-{worker_name}"
    subprocess.run(
        [
            "mvn",
            "archetype:generate",
            "-DgroupId=scc",
            f"-DartifactId={artifact_id}",
            "-DarchetypeArtifactId=maven-archetype-quickstart",
            "-DarchetypeVersion=1.4",
            "-DinteractiveMode=false",
        ],
        cwd="modules",
    ).check_returncode()

    os.rename(
        f"modules/{artifact_id}",
        f"modules/{worker_dir_name}",
    )

    with open(f"modules/{worker_dir_name}/pom.xml", "rb+") as f:
        pom = ET.parse(f)
        name = pom.find("{*}name")
        name.text = "${project.artifactId}"
        dependencies = pom.find("{*}dependencies")
        dependency = ET.SubElement(dependencies, "dependency")
        ET.SubElement(dependency, "groupId").text = "scc"
        ET.SubElement(dependency, "artifactId").text = "scc-backend-kube"
        ET.SubElement(dependency, "version").text = "${project.version}"

        f.seek(0)
        pom.write(f, encoding="utf-8", xml_declaration=True)
        f.truncate()

    with open(f"pom.xml", "rb+") as f:
        pom = ET.parse(f)
        modules = pom.find("{*}modules")
        ET.SubElement(modules, "module").text = f"modules/{worker_dir_name}"

        f.seek(0)
        pom.write(f, encoding="utf-8", xml_declaration=True)
        f.truncate()


if __name__ == "__main__":
    main()
