from typing import Any, Callable, List

import recon
from scc.endpoints import Endpoints
from scc.invalidator import Invalidator

__test_case_factories: List[Callable[[Any], List[recon.TestCase]]] = []


def test_case(name: str, invalidators: List[Invalidator] | None = None) -> Callable:
    def decorator(func) -> Callable:
        if invalidators is None:
            factory = lambda ep: [recon.TestCase(name, lambda: func(ep))]
            register_test_case_factory(factory)
        else:
            for invalidator in invalidators:
                factory = lambda ep, inv=invalidator: [  # type: ignore
                    recon.TestCase(
                        f"{name} {inv.desc}",
                        lambda: func(ep, inv),
                    )
                ]
                register_test_case_factory(factory)
        return func

    return decorator


def test_case_factory() -> Callable:
    def decorator(func) -> Callable:
        register_test_case_factory(func)
        return func

    return decorator


def create_test_cases(endpoints: Endpoints) -> List[recon.TestCase]:
    test_cases = []
    for factory in __test_case_factories:
        test_cases.extend(factory(endpoints))
    return test_cases


def register_test_case_factory(factory: Callable[[Endpoints], List[recon.TestCase]]):
    __test_case_factories.append(factory)


from .media import *
from .user import *
from .auction import *
from .question import *
