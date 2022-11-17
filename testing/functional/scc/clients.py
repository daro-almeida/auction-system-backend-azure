from typing import Union
import requests
from scc import AUTH_COOKIE

from scc.endpoints import Endpoints
from scc.requests import (
    AuctionCreateRequest,
    AuctionUpdateRequest,
    QuestionCreateRequest,
    ReplyCreateRequest,
    UserAuthenticateRequest,
    UserCreateRequest,
    UserUpdateRequest,
)


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

    def list_auctions_of_user(self, user_id: str) -> requests.Response:
        return self.session.get(self.endpoints.user + "/" + user_id + "/auctions")

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
        return self.session.post(
            self.endpoints.auction + "/" + auction_id + "/question",
            json=params.__dict__,
        )

    def create_reply(
        self, auction_id: str, question_id: str, params: ReplyCreateRequest
    ) -> requests.Response:
        return self.session.post(
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

    def create_user_and_auth(
        self, params: Union[UserCreateRequest, None] = None
    ) -> str:
        params = params or UserCreateRequest.random()
        user_id = self.create_user(params)
        self.authenticate_user(UserAuthenticateRequest(user_id, params.password))
        return user_id

    def delete_user(self, user_id: str) -> None:
        response = self.raw.delete_user(user_id)
        assert response.status_code == 204

    def update_user(self, params: UserUpdateRequest) -> None:
        pass

    def list_auctions_of_user(self, user_id: str) -> list[str]:
        response = self.raw.list_auctions_of_user(user_id)
        assert response.status_code == 200
        return response.json()

    def authenticate_user(self, params: UserAuthenticateRequest) -> str:
        response = self.raw.authenticate_user(params)
        assert response.status_code == 200
        assert self.raw.session.cookies.get(AUTH_COOKIE) is not None
        return response.text

    # Auction API
    def create_auction(self, params: Union[AuctionCreateRequest, None] = None) -> str:
        if params == None:
            user_id = self.create_user_and_auth()
            params = AuctionCreateRequest.random()
        response = self.raw.create_auction(params)
        assert response.status_code == 200
        return response.text

    def create_user_and_auction(self) -> tuple[str, str]:
        user_id = self.create_user_and_auth()
        auction_id = self.create_auction(AuctionCreateRequest.random())
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
