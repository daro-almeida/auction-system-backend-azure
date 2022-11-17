import re
import os

RESOURCE_GROUP = "scc2223-rg-westeurope-d464"


def list_function_names() -> list[str]:
    return [
        name.removeprefix("scc-backend-function-")
        for name in os.listdir("modules")
        if re.match(r"^scc-backend-function-[a-z][a-z0-9-]*$", name)
    ]


def function_path(function_name: str) -> str:
    return f"modules/{function_name(function_name)}"


def function_artifact_id(name: str) -> str:
    return f"scc-backend-function-{name}"
