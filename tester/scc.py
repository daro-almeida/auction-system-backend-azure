from __future__ import annotations
from dataclasses import Field, dataclass, replace
from typing import Callable, Generic, Tuple, TypeVar
import requests
from faker import Faker
import base64
import random

T = TypeVar("T")


class Endpoints:
    base: str
    user: str
    media: str
    auction: str

    def __init__(self, base="http://localhost:8080"):
        self.base = base
        self.user = base + "/rest/user"
        self.media = base + "/rest/media"
        self.auction = base + "/rest/auction"


class Invalidator(Generic[T]):
    def __init__(self, field: str, desc: str, func: Callable[[T], T]):
        self.field = field
        self.desc = desc
        self.func = func

    def __str__(self) -> str:
        return f"{self.field}: {self.desc}"


class InvalidRequest(Generic[T]):
    def __init__(self, data: T, reason: str):
        self.data = data
        self.reason = reason

    def __repr__(self) -> str:
        return self.data.__repr__()

    def __str__(self) -> str:
        return f"{self.reason} = {self.data}"


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
class AuctionCreateRequest:
    title: str
    description: str
    userId: str
    endTime: str
    minimumPrice: float
    imageBase64: str

    @staticmethod
    def random(**kwargs):
        faker = Faker()
        request = AuctionCreateRequest(
            title=faker.sentence(),
            description=faker.paragraph(),
            userId=faker.uuid4(),
            endTime=faker.date_time(),
            minimumPrice=faker.pyfloat(),
            imageBase64=random_image_base64(),
        )
        return _patch_dataclass_with_kwargs(request, **kwargs)


@dataclass
class AuctionUpdateRequest:
    title: str
    description: str
    imageBase64: str


class RawClient:
    def __init__(self, endpoints: Endpoints):
        self.endpoints = endpoints

    # Media API
    def upload_media(self, content: bytes) -> requests.Response:
        return requests.post(self.endpoints.media, data=content)

    def download_media(self, media_id: str) -> requests.Response:
        return requests.get(self.endpoints.media + "/" + media_id)

    # User API
    def create_user(self, params: UserCreateRequest) -> requests.Response:
        return requests.post(self.endpoints.user, json=params.__dict__)

    def delete_user(self, user_id: str) -> requests.Response:
        return requests.delete(self.endpoints.user + "/" + user_id)

    def update_user(self, params: UserUpdateRequest) -> requests.Response:
        pass

    # Auction API
    def create_auction(self, params: AuctionCreateRequest) -> requests.Response:
        pass

    def delete_auction(self, auction_id: str) -> requests.Response:
        pass

    def update_auction(self, params: AuctionUpdateRequest) -> requests.Response:
        pass


class Client:
    def __init__(self, endpoints: Endpoints):
        self.rclient = RawClient(endpoints)
        self.endpoints = endpoints

    # Media API
    def upload_media(self, content: bytes) -> str:
        response = self.rclient.upload_media(content)
        assert response.status_code == 200
        return response.text

    def download_media(self, media_id: str) -> bytes:
        response = self.rclient.download_media(media_id)
        assert response.status_code == 200
        return response.content

    # User API
    def create_user(self, params: UserCreateRequest) -> str:
        response = self.rclient.create_user(params)
        return response.text

    def delete_user(self, user_id: str) -> None:
        response = self.rclient.delete_user(user_id)

    def update_user(self, params: UserUpdateRequest) -> None:
        pass

    # Auction API
    def create_auction(self, params: AuctionCreateRequest) -> str:
        pass

    def delete_auction(self, auction_id: str) -> None:
        pass

    def update_auction(self, params: AuctionUpdateRequest) -> None:
        pass


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
