from dataclasses import dataclass
import requests
from faker import Faker
import base64
import random


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


@dataclass
class UserCreateRequest:
    nickname: str
    name: str
    password: str
    imageBase64: str

    @staticmethod
    def random():
        faker = Faker()
        return UserCreateRequest(
            nickname=faker.user_name(),
            name=faker.name(),
            password=faker.password(),
            imageBase64=_random_image_base64(),
        )


@dataclass
class UserUpdateRequest:
    name: str
    password: str
    imageBase64: str

    @staticmethod
    def random():
        faker = Faker()
        return UserUpdateRequest(
            name=faker.name(),
            password=faker.password(),
            imageBase64=_random_image_base64(),
        )


@dataclass
class AuctionCreateRequest:
    title: str
    description: str
    userId: str
    endTime: str
    minimumPrice: float
    imageBase64: str

    @staticmethod
    def random():
        faker = Faker()
        return AuctionCreateRequest(
            title=faker.sentence(),
            description=faker.paragraph(),
            userId=faker.uuid4(),
            endTime=faker.date_time(),
            minimumPrice=faker.pyfloat(),
            imageBase64=_random_image_base64(),
        )


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


def _random_image_base64() -> str:
    return base64.b64encode(random_image()).decode("utf-8")
