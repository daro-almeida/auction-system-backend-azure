from typing import Callable, List

import recon
from scc.endpoints import Endpoints

__test_case_factories = []


def test_case(group: str, name: str) -> Callable:
    def decorator(func) -> Callable:
        factory = lambda ep: [recon.TestCase(group, name, lambda: func(ep))]
        register_test_case_factory(factory)
        return func

    return decorator


def create_test_cases(endpoints: Endpoints) -> List[recon.TestCase]:
    test_cases = []
    for factory in __test_case_factories:
        test_cases.extend(factory(endpoints))
    return test_cases


def register_test_case_factory(factory: Callable[[Endpoints], List[recon.TestCase]]):
    __test_case_factories.append(factory)


from .auction import *
from .media import *
from .question import *
from .user import *
