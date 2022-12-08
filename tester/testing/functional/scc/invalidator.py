from __future__ import annotations

from typing import Callable, Generic, TypeVar


T = TypeVar("T")


class Invalidator(Generic[T]):
    def __init__(self, field: str, desc: str, func: Callable[[T], T]):
        self.field = field
        self.desc = desc
        self.func = func

    def __str__(self) -> str:
        return f"{self.field}: {self.desc}"

    def __call__(self, obj: T) -> T:
        self.func(obj)
        return obj

    @staticmethod
    def null(field: str) -> Invalidator:
        return Invalidator(
            field, f"null {field}", lambda x: _patch_with_kwargs(x, **{field: None})
        )

    @staticmethod
    def empty(field: str) -> Invalidator:
        return Invalidator(
            field, f"empty {field}", lambda x: _patch_with_kwargs(x, **{field: ""})
        )

    @staticmethod
    def negative(field: str) -> Invalidator:
        return Invalidator(
            field,
            f"negative {field}",
            lambda x: _patch_with_kwargs(x, **{field: -x.__getattribute__(field)}),
        )

    @staticmethod
    def zero(field: str) -> Invalidator:
        return Invalidator(field, "zero", lambda x: _patch_with_kwargs(x, **{field: 0}))


class InvalidRequest(Generic[T]):
    def __init__(self, data: T, reason: str):
        self.data = data
        self.reason = reason

    def __repr__(self) -> str:
        return self.data.__repr__()

    def __str__(self) -> str:
        return f"{self.reason} = {self.data}"


def _patch_with_kwargs(data, **kwargs):
    for key, value in kwargs.items():
        data = setattr(data, key, value)
    return data
