from __future__ import annotations
import datetime
import math

from datetime import timezone
from pydantic import BaseModel, Field, validator
from faker import Faker
from .invalidator import Invalidator


class CreateUserRequest(BaseModel):
    id: str
    name: str
    pwd: str
    photoId: str | None = None

    @staticmethod
    def random() -> CreateUserRequest:
        faker = Faker()
        return CreateUserRequest(
            id=faker.user_name(), name=faker.name(), pwd=faker.password(), photoId=None
        )

    @staticmethod
    def invalidators() -> list[Invalidator]:
        return [
            Invalidator.null("id"),
            Invalidator.empty("id"),
            Invalidator.null("name"),
            Invalidator.empty("name"),
            Invalidator.null("pwd"),
            Invalidator.empty("pwd"),
        ]


class AuthenticateUserRequest(BaseModel):
    user: str
    pwd: str

    @staticmethod
    def invalidators() -> list[Invalidator]:
        return [
            Invalidator.null("user"),
            Invalidator.empty("user"),
            Invalidator.null("pwd"),
            Invalidator.empty("pwd"),
        ]


class UpdateUserRequest(BaseModel):
    name: str
    pwd: str
    photoId: str | None = None


class CreateAuctionRequest(BaseModel):
    title: str
    description: str
    owner: str
    minimumPrice: float
    endTime: datetime.datetime
    imageId: str | None = None

    class Config:
        json_encoders = {
            datetime.datetime: lambda dt: dt.astimezone(timezone.utc).isoformat()
        }

    @validator("minimumPrice")
    def check_greater_than_zero(cls, value):
        if value <= 0:
            raise ValueError("Minimum price must be greater than zero")
        return value

    @staticmethod
    def random(owner: str) -> CreateAuctionRequest:
        faker = Faker()
        return CreateAuctionRequest(
            title=faker.sentence(),
            description=faker.text(),
            owner=owner,
            minimumPrice=math.fabs(faker.pyfloat()),
            endTime=faker.future_datetime(),
            imageId=None,
        )

    @staticmethod
    def invalidators() -> list[Invalidator]:
        return [
            Invalidator.null("title"),
            Invalidator.empty("title"),
            Invalidator.null("description"),
            Invalidator.empty("description"),
            Invalidator.null("owner"),
            Invalidator.empty("owner"),
            Invalidator.null("minimumPrice"),
            Invalidator.empty("minimumPrice"),
            Invalidator.negative("minimumPrice"),
            Invalidator.zero("minimumPrice"),
            Invalidator.null("endTime"),
            Invalidator.empty("endTime"),
        ]


class UpdateAuctionRequest(BaseModel):
    title: str
    description: str
    imageId: str


class CreateBidRequest(BaseModel):
    auctionId: str
    user: str
    value: float

    @validator("value")
    def check_greater_than_zero(cls, value):
        if value <= 0:
            raise ValueError("Bid value must be greater than zero")
        return value

    @staticmethod
    def invalidators() -> list[Invalidator]:
        return [
            Invalidator.null("auctionId"),
            Invalidator.empty("auctionId"),
            Invalidator.null("user"),
            Invalidator.empty("user"),
            Invalidator.null("value"),
            Invalidator.empty("value"),
            Invalidator.negative("value"),
            Invalidator.zero("value"),
        ]


class CreateQuestionRequest(BaseModel):
    auctionId: str
    user: str
    text: str

    @staticmethod
    def random(user_id: str, auction_id: str) -> CreateQuestionRequest:
        faker = Faker()
        return CreateQuestionRequest(
            auctionId=auction_id,
            user=user_id,
            text=faker.text(),
        )

    @staticmethod
    def invalidators() -> list[Invalidator]:
        return [
            Invalidator.null("auctionId"),
            Invalidator.empty("auctionId"),
            Invalidator.null("user"),
            Invalidator.empty("user"),
            Invalidator.null("text"),
            Invalidator.empty("text"),
        ]


class CreateReplyRequest(BaseModel):
    reply: str

    @staticmethod
    def random() -> CreateReplyRequest:
        faker = Faker()
        return CreateReplyRequest(reply=faker.text())

    @staticmethod
    def invalidators() -> list[Invalidator]:
        return [
            Invalidator.null("reply"),
            Invalidator.empty("reply"),
        ]
