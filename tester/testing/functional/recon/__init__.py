from __future__ import annotations

from dataclasses import dataclass
import json
from typing import Callable, List, Union
from colorama import Fore, Style
import requests
import re

_last_validator: Validator | None = None


@dataclass
class TestCase:
    name: str
    runnable: Callable

    def __init__(self, name: str, runnable: Callable):
        self.name = name
        self.runnable = runnable


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

    def status_code_failure(self):
        if not (self.response.status_code >= 400 and self.response.status_code < 500):
            raise AssertionError(
                self.response,
                f"Expected status code to be in range 400-499, got {self.response.status_code}",
            )

    def status_code_success(self):
        if not (self.response.status_code >= 200 and self.response.status_code < 300):
            raise AssertionError(
                self.response,
                f"Expected status code to be in range 200-299, got {self.response.status_code}",
            )

    def status_code(self, expected: int | List[int]):
        valid = False
        if isinstance(expected, int):
            valid = self.response.status_code == expected
        else:
            valid = self.response.status_code in expected
        if not valid:
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
                f"Expected content {expected!r}, got {self.response.content!r}",
            )

    def cookie_exists(self, name: str):
        if name not in self.response.cookies:
            raise AssertionError(self.response, f"Expected cookie {name} to exist")

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
        global _last_validator
        _last_validator = self
        return self

    def __exit__(self, type, value, traceback):
        pass


def test_case(name: str) -> Callable[[Callable], TestCase]:
    def decorator(func):
        return lambda *args, **kwargs: TestCase(name, lambda: func(*args, **kwargs))

    return decorator


def run(
    test_cases: List[TestCase], filter: str | None = None, ignore_errors: bool = False
):
    global _last_validator
    for test_case in test_cases:
        _last_validator = None
        if filter is not None and not re.match(filter, test_case.name):
            continue
        try:
            test_case.runnable()
            print(f"[{Fore.GREEN}OK{Style.RESET_ALL}] {test_case.name}")
        except AssertionError as e:
            print(f"[{Fore.RED}FAIL{Style.RESET_ALL}] {test_case.name}")
            print(e.message)
            print("-------------------- Request --------------------")
            print(e.response.request.method, e.response.request.url)
            print(e.response.request.headers)

            if e.response.request.body is not None:
                try:
                    jbody = json.loads(e.response.request.body)
                    if jbody is not None:
                        for k in jbody.keys():
                            if len(json.dumps(jbody[k])) > 1024:
                                jbody[k] = "..."
                        print(json.dumps(jbody, indent=4))
                except:
                    if len(e.response.request.body) > 4096 * 10:
                        print(f"Body too large to print {len(e.response.request.body)}")
                    else:
                        print(e.response.request.body)

            print("-------------------- Response --------------------")
            print(e.response.status_code, e.response.reason)
            print(e.response.headers)
            print(e.response.text)
            print("-------------------------------------------------")

            if not ignore_errors:
                break
        except Exception as e:
            if _last_validator is None:
                print(f"[{Fore.RED}FAIL{Style.RESET_ALL}] {test_case.name}")
                if not ignore_errors:
                    raise e
            else:
                response = _last_validator.response
                print(f"[{Fore.RED}FAIL{Style.RESET_ALL}] {test_case.name}")
                print("-------------------- Request --------------------")
                print(response.request.method, response.request.url)
                print(response.request.headers)

                if response.request.body is not None:
                    try:
                        jbody = json.loads(response.request.body)
                        if jbody is not None:
                            for k in jbody.keys():
                                if len(json.dumps(jbody[k])) > 1024:
                                    jbody[k] = "..."
                            print(json.dumps(jbody, indent=4))
                    except:
                        if len(response.request.body) > 4096 * 10:
                            print(
                                f"Body too large to print {len(response.request.body)}"
                            )
                        else:
                            print(response.request.body)

                print("-------------------- Response --------------------")
                print(response.status_code, response.reason)
                print(response.headers)
                print(response.text)
                print("-------------------------------------------------")
                print(e)

                if not ignore_errors:
                    break


def validate(response: requests.Response) -> Validator:
    return Validator(response)
