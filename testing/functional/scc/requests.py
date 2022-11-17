from __future__ import annotations

import math

from dataclasses import dataclass, replace
from faker import Faker

from scc import (
    _patch_dataclass_with_kwargs,
    invalid_image_base64,
    random_image_base64,
)
from scc.invalidator import InvalidRequest, Invalidator


@dataclass
class UserCreateRequest:
    nickname: str
    name: str
    password: str
    imageBase64: str

    @staticmethod
    def invalid_requests() -> list[InvalidRequest[UserCreateRequest]]:
        invalidators = UserCreateRequest._invalidators()
        invalid_requests = []
        for invalidator in invalidators:
            rand = UserCreateRequest.random()
            invalid_requests.append(
                InvalidRequest(
                    data=invalidator.func(rand),
                    reason=f"{invalidator.field} {invalidator.desc}",
                )
            )
        return invalid_requests

    @staticmethod
    def random(**kwargs):
        faker = Faker()
        request = UserCreateRequest(
            nickname=faker.user_name(),
            name=faker.name(),
            password=faker.password(),
            imageBase64=random_image_base64(),
        )
        return _patch_dataclass_with_kwargs(request, **kwargs)

    @staticmethod
    def _invalidators() -> list[Invalidator[UserCreateRequest]]:
        return [
            Invalidator(
                "nickname",
                "empty",
                lambda req: _patch_dataclass_with_kwargs(req, nickname=""),
            ),
            Invalidator(
                "nickname",
                "null",
                lambda req: _patch_dataclass_with_kwargs(req, nickname=None),
            ),
            Invalidator(
                "name",
                "empty",
                lambda req: _patch_dataclass_with_kwargs(req, name=""),
            ),
            Invalidator(
                "name",
                "null",
                lambda req: _patch_dataclass_with_kwargs(req, name=None),
            ),
            Invalidator(
                "password",
                "empty",
                lambda req: _patch_dataclass_with_kwargs(req, password=""),
            ),
            Invalidator(
                "password",
                "null",
                lambda req: _patch_dataclass_with_kwargs(req, password=None),
            ),
            Invalidator(
                "imageBase64",
                "invalid",
                lambda req: _patch_dataclass_with_kwargs(
                    req, imageBase64=invalid_image_base64()
                ),
            ),
        ]


@dataclass
class UserUpdateRequest:
    name: str
    password: str
    imageBase64: str

    @staticmethod
    def random(**kwargs):
        faker = Faker()
        request = UserUpdateRequest(
            name=faker.name(),
            password=faker.password(),
            imageBase64=random_image_base64(),
        )
        return _patch_dataclass_with_kwargs(request, **kwargs)


@dataclass
class UserAuthenticateRequest:
    nickname: str
    password: str

    @staticmethod
    def random(**kwargs):
        faker = Faker()
        request = UserAuthenticateRequest(
            nickname=faker.user_name(), password=faker.password()
        )
        return _patch_dataclass_with_kwargs


@dataclass
class AuctionCreateRequest:
    title: str
    description: str
    initialPrice: float
    endTime: str
    imageBase64: str

    @staticmethod
    def random(**kwargs):
        faker = Faker()
        request = AuctionCreateRequest(
            title=faker.sentence(),
            description=faker.paragraph(),
            endTime=faker.future_date().isoformat(),
            initialPrice=math.fabs(faker.pyfloat()),
            imageBase64=random_image_base64(),
        )
        return _patch_dataclass_with_kwargs(request, **kwargs)

    @staticmethod
    def invalid_requests() -> list[InvalidRequest[AuctionCreateRequest]]:
        invalidators = AuctionCreateRequest._invalidators()
        invalid_requests = []
        for invalidator in invalidators:
            rand = AuctionCreateRequest.random()
            invalid_requests.append(
                InvalidRequest(
                    data=invalidator.func(rand),
                    reason=f"{invalidator.field} {invalidator.desc}",
                )
            )
        return invalid_requests

    @staticmethod
    def _invalidators() -> list[Invalidator[AuctionCreateRequest]]:
        return [
            Invalidator.empty("title"),
            Invalidator.null("title"),
            Invalidator.empty("description"),
            Invalidator.null("description"),
            Invalidator(
                "initialPrice", "negative", lambda req: replace(req, initialPrice=-1)
            ),
            Invalidator.empty("endTime"),
            Invalidator.null("endTime"),
        ]


@dataclass
class AuctionUpdateRequest:
    title: str
    description: str
    imageBase64: str


@dataclass
class QuestionCreateRequest:
    question: str

    @staticmethod
    def random(**kwargs):
        faker = Faker()
        request = QuestionCreateRequest(
            question=faker.sentence(),
        )
        return _patch_dataclass_with_kwargs(request, **kwargs)


@dataclass
class ReplyCreateRequest:
    reply: str

    @staticmethod
    def random(**kwargs):
        faker = Faker()
        request = ReplyCreateRequest(
            reply=faker.sentence(),
        )
        return _patch_dataclass_with_kwargs(request, **kwargs)
