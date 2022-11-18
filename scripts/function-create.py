#!/usr/bin/python3
import os
import re
import subprocess
import argparse
import xml.etree.ElementTree as ET
import lib.azure as azure


def assert_function_name_is_valid(function_name):
    matches = re.match(r"^[a-z][a-z0-9-]*$", function_name)
    if not matches:
        raise ValueError(
            "Function name must be lowercase, alphanumeric and may contain hyphens"
        )


def main():
    args = argparse.ArgumentParser()
    args.add_argument("function_name")
    args = args.parse_args()

    assert_function_name_is_valid(args.function_name)
    ET.register_namespace("", "http://maven.apache.org/POM/4.0.0")

    subprocess.run(
        [
            "mvn",
            "-B",
            "archetype:generate",
            "-DarchetypeGroupId=com.microsoft.azure",
            "-DarchetypeArtifactId=azure-functions-archetype",
            "-DjavaVersion=17",
            "-DgroupId=scc.azure.functions",
            "-DappName=${project.artifactId}",
            f"-DresourceGroup={azure.RESOURCE_GROUP}",
            f"-DappRegion=${azure.REGION}",
            f"-DartifactId=${azure.function_artifact_id(args.function_name)}",
        ],
        cwd="modules",
    ).check_returncode()

    os.rename(
        azure.function_artifact_id(args.function_name),
        azure.function_directory_name(args.function_name),
    )

    with open(
        f"modules/{azure.function_directory_name(args.function_name)}/pom.xml", "rb+"
    ) as f:
        pom = ET.parse(f)
        properties = pom.find("{*}properties")
        ET.SubElement(properties, "functionPrincingTier").text = "B1"
        dependencies = pom.find("{*}dependencies")
        dependency = ET.SubElement(dependencies, "dependency")
        ET.SubElement(dependency, "groupId").text = "scc"
        ET.SubElement(dependency, "artifactId").text = "scc-backend-azure"
        ET.SubElement(dependency, "version").text = "${project.version}"

        f.seek(0)
        pom.write(f, encoding="utf-8", xml_declaration=True)
        f.truncate()

    with open(f"pom.xml", "rb+") as f:
        pom = ET.parse(f)
        modules = pom.find("{*}modules")
        ET.SubElement(
            modules, "module"
        ).text = f"modules/{azure.function_directory_name(args.function_name)}"

        f.seek(0)
        pom.write(f, encoding="utf-8", xml_declaration=True)
        f.truncate()


if __name__ == "__main__":
    main()