from __future__ import annotations

from typing import Callable, Generic, TypeVar

from scc import _patch_dataclass_with_kwargs


T = TypeVar("T")


class Invalidator(Generic[T]):
    def __init__(self, field: str, desc: str, func: Callable[[T], T]):
        self.field = field
        self.desc = desc
        self.func = func

    def __str__(self) -> str:
        return f"{self.field}: {self.desc}"

    @staticmethod
    def null(field: str) -> Invalidator:
        return Invalidator(
            field, "null", lambda x: _patch_dataclass_with_kwargs(x, **{field: None})
        )

    @staticmethod
    def empty(field: str) -> Invalidator:
        return Invalidator(
            field, "empty", lambda x: _patch_dataclass_with_kwargs(x, **{field: ""})
        )


class InvalidRequest(Generic[T]):
    def __init__(self, data: T, reason: str):
        self.data = data
        self.reason = reason

    def __repr__(self) -> str:
        return self.data.__repr__()

    def __str__(self) -> str:
        return f"{self.reason} = {self.data}"
