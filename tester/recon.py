from dataclasses import dataclass
import json
from typing import Callable, List, Union
from colorama import Fore, Style
import requests


@dataclass
class TestCase:
    group: str
    name: str
    runnable: Callable

    def __init__(self, group: str, name: str, runnable: Callable):
        self.group = group
        self.name = name
        self.runnable = runnable


class TestGroup:
    _tests: List[TestCase]

    def __init__(self, tests: List[TestCase]):
        self._tests = tests


class AssertionError(Exception):
    response: requests.Response
    message: str

    def __init__(self, response: requests.Response, message: str):
        self.response = response
        self.message = message


class Validator:
    response: requests.Response

    def __init__(self, response: requests.Response):
        self.response = response

    def status_code(self, expected: int):
        if self.response.status_code != expected:
            raise AssertionError(
                self.response,
                f"Expected status code {expected}, got {self.response.status_code}",
            )

    def content_type(self, expected: str):
        if self.response.headers["Content-Type"] != expected:
            raise AssertionError(
                self.response,
                f"Expected content type {expected}, got {self.response.headers['Content-Type']}",
            )

    def content(self, expected: bytes):
        if self.response.content != expected:
            raise AssertionError(
                self.response,
                f"Expected content {expected}, got {self.response.content}",
            )

    def equals(self, actual, expected, message: Union[str, None]):
        if actual != expected:
            raise AssertionError(
                self.response, f"Expected {expected}, got {actual}. {message}"
            )

    def not_equals(self, actual, expected, message: Union[str, None]):
        if actual == expected:
            raise AssertionError(
                self.response, f"Expected {expected} to not equal {actual}. {message}"
            )

    def fail(self, message: str):
        raise AssertionError(self.response, "Failed: " + message)

    def __enter__(self):
        return self

    def __exit__(self, type, value, traceback):
        pass


def test_case(group: str, name: str) -> Callable[[Callable], TestCase]:
    def decorator(func):
        return lambda *args, **kwargs: TestCase(
            group, name, lambda: func(*args, **kwargs)
        )

    return decorator


def run(**kwargs):
    groups = kwargs.get("groups", None)
    filters = kwargs.get("filters", None)
    for test_case in [test for group in groups for test in group._tests]:
        if filters is None or len(filters) == 0 or test_case.group in filters:
            try:
                test_case.runnable()
                print(
                    f"[{Fore.GREEN}OK{Style.RESET_ALL}] {test_case.group}/{test_case.name}"
                )
            except AssertionError as e:
                print(
                    f"[{Fore.RED}FAIL{Style.RESET_ALL}] {test_case.group}/{test_case.name}"
                )
                print(e.message)
                print("-------------------- Request --------------------")
                print(e.response.request.method, e.response.request.url)
                print(e.response.request.headers)

                if e.response.request.body is not None:
                    jbody = json.loads(e.response.request.body)
                    if jbody is not None:
                        for k in jbody.keys():
                            if len(json.dumps(jbody[k])) > 1024:
                                jbody[k] = "..."
                        print(json.dumps(jbody, indent=4))
                    elif len(e.response.request.body) > 4096 * 10:
                        print(f"Body too large to print {len(e.response.request.body)}")
                    else:
                        print(e.response.request.body)

                print("-------------------- Response --------------------")
                print(e.response.status_code, e.response.reason)
                print(e.response.headers)
                print(e.response.text)
                print("-------------------------------------------------")


def validate(response: requests.Response) -> Validator:
    return Validator(response)
