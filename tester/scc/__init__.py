import base64
import random

from dataclasses import replace

AUTH_COOKIE = "scc:session"


def random_image() -> bytes:
    return random.randbytes(64)


def random_image_base64() -> str:
    return base64.b64encode(random_image()).decode("utf-8")


def invalid_image_base64() -> str:
    return "&??????"


def _patch_dataclass_with_kwargs(data, **kwargs):
    for key, value in kwargs.items():
        data = replace(data, **{key: value})
    return data
