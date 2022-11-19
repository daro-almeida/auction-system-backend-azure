from datetime import timedelta
import time
import recon
from scc.clients import Client, RawClient
from scc.endpoints import Endpoints
from scc.requests import *
from scc.responses import *
from . import test_case


@test_case("bid/create")
def create_bid(endpoints: Endpoints):
    client = Client(endpoints)
    user, auction = client.create_user_and_auction()
    request = CreateBidRequest(auctionId=auction.id, user=user.id, value=100)
    response = client.raw.create_bid(request)
    with recon.validate(response) as validator:
        validator.status_code(200)
        if len(response.text) == 0:
            validator.fail("response body is empty")
        bid = BidDTO.parse_obj(response.json())
        validator.equals(bid.auctionId, auction.id, "bid auction")
        validator.equals(bid.user, user.id, "bid user")
        validator.equals(bid.value, request.value, "bid value")


@test_case("bid/create two")
def create_two_bids(endpoints: Endpoints):
    client = Client(endpoints)
    user, auction = client.create_user_and_auction()
    client.create_bid(CreateBidRequest(auctionId=auction.id, user=user.id, value=100))

    request = CreateBidRequest(auctionId=auction.id, user=user.id, value=200)
    response = client.raw.create_bid(request)
    with recon.validate(response) as validator:
        validator.status_code(200)
        if len(response.text) == 0:
            validator.fail("response body is empty")
        bid = BidDTO.parse_obj(response.json())
        validator.equals(bid.auctionId, auction.id, "bid auction")
        validator.equals(bid.user, user.id, "bid user")
        validator.equals(bid.value, request.value, "bid value")


@test_case("bid/create equal price")
def create_equal_price_bid(endpoints: Endpoints):
    client = Client(endpoints)
    user, auction = client.create_user_and_auction()
    client.create_bid(CreateBidRequest(auctionId=auction.id, user=user.id, value=100))

    request = CreateBidRequest(auctionId=auction.id, user=user.id, value=100)
    response = client.raw.create_bid(request)
    with recon.validate(response) as validator:
        validator.status_code_failure()


@test_case("bid/create lower price")
def create_lower_price_bid(endpoints: Endpoints):
    client = Client(endpoints)
    user, auction = client.create_user_and_auction()
    client.create_bid(CreateBidRequest(auctionId=auction.id, user=user.id, value=100))

    request = CreateBidRequest(auctionId=auction.id, user=user.id, value=50)
    response = client.raw.create_bid(request)
    with recon.validate(response) as validator:
        validator.status_code_failure()


@test_case("bid/create and check auction")
def create_bid_and_check_auction(endpoints: Endpoints):
    client = Client(endpoints)
    user, auction = client.create_user_and_auction()

    client.create_bid(CreateBidRequest(auctionId=auction.id, user=user.id, value=100))
    a1 = client.get_auction(auction.id)
    assert a1.bid is not None
    assert a1.bid.value == 100

    client.create_bid(CreateBidRequest(auctionId=auction.id, user=user.id, value=200))
    a2 = client.get_auction(auction.id)
    assert a2.bid is not None
    assert a2.bid.value == 200


@test_case("bid/bid on closed auction")
def bid_on_closed_auction(endpoints: Endpoints):
    client = Client(endpoints)
    user_id = client.create_user_and_auth().id
    request = CreateAuctionRequest.random(user_id)
    request.endTime = datetime.datetime.now() + timedelta(seconds=5)
    auction = client.create_auction(request)
    bid1 = client.create_bid(
        CreateBidRequest(auctionId=auction.id, user=user_id, value=100)
    )
    time.sleep(10)
    response = client.raw.create_bid(
        CreateBidRequest(auctionId=auction.id, user=user_id, value=200)
    )
    auction = client.get_auction(auction.id)
    assert auction.bid is not None
    assert auction.bid.value == 100
    assert auction.bid.id == bid1.id
    with recon.validate(response) as validator:
        validator.status_code_failure()
