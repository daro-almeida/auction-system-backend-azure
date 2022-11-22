import re
import os

REGION = "westeurope"
RESOURCE_GROUP = "scc-backend-d464-rg"


def list_function_names() -> list[str]:
    return [
        name.removeprefix("function-")
        for name in os.listdir("modules")
        if re.match(r"^function-[a-z][a-z0-9-]*$", name)
    ]


def function_path(function_name: str) -> str:
    return f"modules/{function_directory_name(function_name)}"


def function_artifact_id(name: str) -> str:
    return f"scc-backend-azure-function-{name}"


def function_directory_name(name: str) -> str:
    return f"function-{name}"
