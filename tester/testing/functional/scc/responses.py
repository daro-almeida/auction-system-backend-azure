from __future__ import annotations

import datetime
from enum import Enum, StrEnum
from pydantic import BaseModel, Field, validator


class CreateUserResponse(BaseModel):
    id: str
    name: str
    pwd: str
    photoId: str | None = None


class UserDAO(BaseModel):
    id: str
    name: str
    photoId: str | None = None


class AuctionStatus(StrEnum):
    OPEN = "OPEN"
    CLOSED = "CLOSED"


class BidDTO(BaseModel):
    id: str
    auctionId: str
    user: str
    time: datetime.datetime
    value: float

    @validator("value")
    def check_greater_than_zero(cls, value):
        if value <= 0:
            raise ValueError("Bid value must be greater than zero")
        return value


class AuctionDTO(BaseModel):
    id: str
    title: str
    description: str
    owner: str
    imageId: str | None = None
    endTime: datetime.datetime
    minimumPrice: float
    status: AuctionStatus
    bid: BidDTO | None = None


class QuestionDTO(BaseModel):
    id: str
    auctionId: str
    authorId: str
    text: str
    reply: str | None = None
