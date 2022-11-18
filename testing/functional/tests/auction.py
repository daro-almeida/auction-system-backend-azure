from typing import Generator
import recon
from scc.clients import Client, RawClient
from scc.endpoints import Endpoints
from scc.requests import *
from scc.responses import *
from . import test_case


@test_case("auction/create")
def create_auction(endpoints: Endpoints):
    client = Client(endpoints)
    user_id = client.create_user_and_auth().id
    request = CreateAuctionRequest.random(user_id)
    response = client.raw.create_auction(request)
    with recon.validate(response) as validator:
        validator.status_code(200)
        auction = AuctionDTO.parse_obj(response.json())
        validator.equals(auction.title, request.title, "auction title")
        validator.equals(
            auction.description, request.description, "auction description"
        )
        validator.equals(auction.owner, user_id, "auction user id")
        validator.equals(
            auction.minimumPrice, request.minimumPrice, "auction minimum price"
        )


@test_case("auction/create with invalid user")
def create_auction_with_invalid_user(endpoints: Endpoints):
    client = Client(endpoints)
    response = client.raw.create_auction(CreateAuctionRequest.random("invalid-user"))
    with recon.validate(response) as validator:
        validator.status_code(401)


@test_case("auction/get user auctions")
def get_user_auctions(endpoints: Endpoints):
    client = Client(endpoints)
    user_id = client.create_user_and_auth().id
    auction_params = [CreateAuctionRequest.random(user_id) for _ in range(5)]
    auction_ids = [client.create_auction(p) for p in auction_params]
    response = client.raw.list_user_auctions(user_id)
    with recon.validate(response) as validator:
        validator.status_code(200)


@test_case(
    "auction/create invalid with", invalidators=CreateAuctionRequest.invalidators()
)
def create_auction_invalid_with(
    endpoints: Endpoints, invalidator: Invalidator[CreateAuctionRequest]
):
    client = Client(endpoints)
    user_id = client.create_user_and_auth().id
    request = invalidator(CreateAuctionRequest.random(user_id))
    response = client.raw.create_auction(request)
    with recon.validate(response) as validator:
        validator.status_code([400, 401])
