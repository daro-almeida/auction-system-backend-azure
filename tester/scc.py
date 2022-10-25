from __future__ import annotations
from dataclasses import Field, dataclass, replace
import math
from typing import Callable, Generic, Tuple, TypeVar, Union
import requests
from faker import Faker
import base64
import random

T = TypeVar("T")


class Endpoints:
    base: str
    user: str
    user_auth: str
    media: str
    auction: str

    def __init__(self, base="http://localhost:8080"):
        self.base = base
        self.user = base + "/rest/user"
        self.user_auth = base + "/rest/user/auth"
        self.media = base + "/rest/media"
        self.auction = base + "/rest/auction"


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
    userId: str
    initialPrice: float
    endTime: str
    imageBase64: str

    @staticmethod
    def random(user_id: str, **kwargs):
        faker = Faker()
        request = AuctionCreateRequest(
            title=faker.sentence(),
            description=faker.paragraph(),
            userId=user_id,
            endTime=faker.future_date().isoformat(),
            initialPrice=math.fabs(faker.pyfloat()),
            imageBase64=random_image_base64(),
        )
        return _patch_dataclass_with_kwargs(request, **kwargs)

    @staticmethod
    def invalid_requests(user_id: str) -> list[InvalidRequest[AuctionCreateRequest]]:
        invalidators = AuctionCreateRequest._invalidators()
        invalid_requests = []
        for invalidator in invalidators:
            rand = AuctionCreateRequest.random(user_id)
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
    userId: str
    question: str

    @staticmethod
    def random(user_id: str, **kwargs):
        faker = Faker()
        request = QuestionCreateRequest(
            userId=user_id,
            question=faker.sentence(),
        )
        return _patch_dataclass_with_kwargs(request, **kwargs)


@dataclass
class ReplyCreateRequest:
    userId: str
    reply: str

    @staticmethod
    def random(user_id: str, **kwargs):
        faker = Faker()
        request = ReplyCreateRequest(
            userId=user_id,
            reply=faker.sentence(),
        )
        return _patch_dataclass_with_kwargs(request, **kwargs)


class RawClient:
    def __init__(self, endpoints: Endpoints):
        self.session = requests.Session()
        self.endpoints = endpoints

    # Media API
    def upload_media(self, content: bytes) -> requests.Response:
        return self.session.post(self.endpoints.media, data=content)

    def download_media(self, media_id: str) -> requests.Response:
        return self.session.get(self.endpoints.media + "/" + media_id)

    # User API
    def create_user(self, params: UserCreateRequest) -> requests.Response:
        return self.session.post(self.endpoints.user, json=params.__dict__)

    def delete_user(self, user_id: str) -> requests.Response:
        return self.session.delete(self.endpoints.user + "/" + user_id)

    def update_user(self, params: UserUpdateRequest) -> requests.Response:
        pass

    def authenticate_user(self, params: UserAuthenticateRequest) -> requests.Response:
        return self.session.post(self.endpoints.user_auth, json=params.__dict__)

    # Auction API
    def create_auction(self, params: AuctionCreateRequest) -> requests.Response:
        return self.session.post(self.endpoints.auction, json=params.__dict__)

    def update_auction(self, params: AuctionUpdateRequest) -> requests.Response:
        pass

    # Question API
    def create_question(
        self, auction_id: str, params: QuestionCreateRequest
    ) -> requests.Response:
        return requests.post(
            self.endpoints.auction + "/" + auction_id + "/question",
            json=params.__dict__,
        )

    def create_reply(
        self, auction_id: str, question_id: str, params: ReplyCreateRequest
    ) -> requests.Response:
        return requests.post(
            self.endpoints.auction + "/" + auction_id + "/question/" + question_id,
            json=params.__dict__,
        )


class Client:
    def __init__(self, rawclient_or_endpoints: Union[Endpoints, RawClient]):
        if isinstance(rawclient_or_endpoints, RawClient):
            self.raw = rawclient_or_endpoints
            self._endpoints = rawclient_or_endpoints.endpoints
        else:
            self.raw = RawClient(rawclient_or_endpoints)
            self._endpoints = rawclient_or_endpoints

    # Media API
    def upload_media(self, content: bytes) -> str:
        response = self.raw.upload_media(content)
        assert response.status_code == 200
        return response.text

    def download_media(self, media_id: str) -> bytes:
        response = self.raw.download_media(media_id)
        assert response.status_code == 200
        return response.content

    # User API
    def create_user(self, params: Union[UserCreateRequest, None] = None) -> str:
        response = self.raw.create_user(params or UserCreateRequest.random())
        assert response.status_code == 200
        return response.text

    def delete_user(self, user_id: str) -> None:
        response = self.raw.delete_user(user_id)
        assert response.status_code == 204

    def update_user(self, params: UserUpdateRequest) -> None:
        pass

    def authenticate_user(self, params: UserAuthenticateRequest) -> str:
        response = self.raw.authenticate_user(params)
        assert response.status_code == 200
        return response.text

    # Auction API
    def create_auction(self, params: Union[AuctionCreateRequest, None] = None) -> str:
        if params == None:
            user_id = self.create_user()
            params = AuctionCreateRequest.random(user_id)
        response = self.raw.create_auction(params)
        assert response.status_code == 200
        return response.text

    def create_user_and_auction(self) -> tuple[str, str]:
        user_id = self.create_user()
        auction_id = self.create_auction(AuctionCreateRequest.random(user_id))
        return user_id, auction_id

    def update_auction(self, params: AuctionUpdateRequest) -> None:
        pass

    # Question API
    def create_question(self, auction_id: str, params: QuestionCreateRequest) -> str:
        response = self.raw.create_question(auction_id, params)
        assert response.status_code == 200
        return response.text

    def create_reply(
        self, auction_id: str, question_id: str, params: ReplyCreateRequest
    ) -> None:
        response = self.raw.create_reply(auction_id, question_id, params)
        assert response.status_code == 204


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
