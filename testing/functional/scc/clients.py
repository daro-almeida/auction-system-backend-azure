from typing import Union
import requests
from scc import AUTH_COOKIE

from scc.endpoints import Endpoints
from scc.requests import (
    AuthenticateUserRequest,
    CreateAuctionRequest,
    CreateBidRequest,
    CreateQuestionRequest,
    CreateReplyRequest,
    CreateUserRequest,
    UpdateAuctionRequest,
    UpdateUserRequest,
)
from scc.responses import AuctionDTO, CreateUserResponse, QuestionDTO


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
    def get_user(self, user_id: str) -> requests.Response:
        return self.session.get(self.endpoints.user + "/" + user_id)

    def create_user(self, params: CreateUserRequest) -> requests.Response:
        return self.session.post(self.endpoints.user, json=params.dict())

    def authenticate_user(self, params: AuthenticateUserRequest) -> requests.Response:
        return self.session.post(self.endpoints.user_auth, json=params.dict())

    def update_user(self, params: UpdateUserRequest) -> requests.Response:
        return self.session.patch(self.endpoints.user, json=params.dict())

    def delete_user(self, user_id: str) -> requests.Response:
        return self.session.delete(self.endpoints.user + "/" + user_id)

    def list_user_auctions(self, user_id: str) -> requests.Response:
        return self.session.get(self.endpoints.user + "/" + user_id + "/auctions")

    def list_user_followed_auctions(self, user_id: str) -> requests.Response:
        return self.session.get(self.endpoints.user + "/" + user_id + "/following")

    # Auction API
    def create_auction(self, params: CreateAuctionRequest) -> requests.Response:
        return self.session.post(
            self.endpoints.auction,
            headers={"Content-Type": "application/json"},
            data=params.json(),
        )

    def update_auction(
        self, auction_id: str, params: UpdateAuctionRequest
    ) -> requests.Response:
        return self.session.patch(
            self.endpoints.auction + f"/{auction_id}", json=params.dict()
        )

    def create_bid(self, params: CreateBidRequest) -> requests.Response:
        return self.session.post(
            f"{self.endpoints.auction}/{params.auctionId}/bid", json=params.dict()
        )

    def list_bids(self, auction_id: str) -> requests.Response:
        return self.session.get(f"{self.endpoints.auction}/{auction_id}/bid")

    # Question API
    def create_question(
        self, auction_id: str, params: CreateQuestionRequest
    ) -> requests.Response:
        return self.session.post(
            f"{self.endpoints.auction}/{auction_id}/question", json=params.dict()
        )

    def create_reply(
        self, auction_id: str, question_id: str, params: CreateReplyRequest
    ) -> requests.Response:
        return self.session.post(
            f"{self.endpoints.auction}/{auction_id}/question/{question_id}/reply",
            json=params.dict(),
        )

    def list_questions(self, auction_id: str) -> requests.Response:
        return self.session.get(self.endpoints.auction + "/" + auction_id + "/question")

    def list_auctions_about_to_close(self) -> requests.Response:
        return self.session.get(self.endpoints.auction + "/any/soon-to-close")

    def list_recent_auctions(self) -> requests.Response:
        return self.session.get(self.endpoints.auction + "/any/recent")

    def list_popular_auctions(self) -> requests.Response:
        return self.session.get(self.endpoints.auction + "/any/popular")


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
    def create_user(
        self, params: CreateUserRequest | None = None
    ) -> CreateUserResponse:
        response = self.raw.create_user(params or CreateUserRequest.random())
        assert response.status_code == 200
        return CreateUserResponse.parse_obj(response.json())

    def create_user_and_auth(
        self, params: CreateUserRequest | None = None
    ) -> CreateUserResponse:
        params = params or CreateUserRequest.random()
        resp = self.create_user(params)
        self.authenticate_user(AuthenticateUserRequest(user=params.id, pwd=params.pwd))
        return resp

    def delete_user(self, user_id: str) -> None:
        response = self.raw.delete_user(user_id)
        assert response.status_code == 204

    def update_user(self, params: UpdateUserRequest) -> None:
        response = self.raw.update_user(params)
        assert response.status_code == 204

    def list_auctions_of_user(self, user_id: str) -> list[AuctionDTO]:
        response = self.raw.list_user_auctions(user_id)
        assert response.status_code == 200
        return [AuctionDTO.parse_obj(v) for v in response.json()]

    def authenticate_user(self, params: AuthenticateUserRequest):
        response = self.raw.authenticate_user(params)
        assert response.status_code == 200
        assert self.raw.session.cookies.get(AUTH_COOKIE) is not None
        assert len(response.text) == 0

    # Auction API
    def create_auction(self, params: CreateAuctionRequest) -> AuctionDTO:
        response = self.raw.create_auction(params)
        assert response.status_code == 200
        return AuctionDTO.parse_obj(response.json())

    def create_user_and_auction(self) -> tuple[CreateUserResponse, AuctionDTO]:
        user = self.create_user_and_auth()
        auction = self.create_auction(CreateAuctionRequest.random(user.id))
        return user, auction

    def update_auction(self, auction_id: str, params: UpdateAuctionRequest) -> None:
        response = self.raw.update_auction(auction_id, params)
        assert response.status_code == 204

    # Question API
    def create_question(
        self, auction_id: str, params: CreateQuestionRequest
    ) -> QuestionDTO:
        response = self.raw.create_question(auction_id, params)
        assert response.status_code == 200
        return QuestionDTO.parse_obj(response.json())

    def create_random_question(self, user_id: str, auction_id: str) -> QuestionDTO:
        return self.create_question(
            auction_id, CreateQuestionRequest.random(user_id, auction_id)
        )

    def create_reply(
        self, auction_id: str, question_id: str, params: CreateReplyRequest
    ) -> str:
        response = self.raw.create_reply(auction_id, question_id, params)
        assert response.status_code == 204
        return response.text
