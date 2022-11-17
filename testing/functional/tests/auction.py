import recon
from scc.clients import Client, RawClient
from scc.endpoints import Endpoints
from scc.requests import AuctionCreateRequest
from . import register_test_case_factory, test_case


@test_case("auction", "create auction")
def create_auction(endpoints: Endpoints):
    client = Client(endpoints)
    user_id = client.create_user_and_auth()
    response = client.raw.create_auction(AuctionCreateRequest.random())
    with recon.validate(response) as validator:
        validator.status_code(200)


@test_case("auction", "create auction with invalid user")
def create_auction_with_invalid_user(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    response = rclient.create_auction(AuctionCreateRequest.random())
    with recon.validate(response) as validator:
        validator.status_code(401)


@test_case("auction", "get user auctions")
def get_user_auctions(endpoints: Endpoints):
    client = Client(endpoints)
    user_id = client.create_user_and_auth()
    auction_params = [AuctionCreateRequest.random() for _ in range(5)]
    auction_ids = [client.create_auction(p) for p in auction_params]
    response = client.raw.list_auctions_of_user(user_id)
    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.equals(set(response.json()), set(auction_ids), "auction ids")


def invalid_create_test_cases(endpoints: Endpoints) -> list[recon.TestCase]:
    def test_case(ep: Endpoints, req: AuctionCreateRequest):
        client = Client(endpoints)
        user_id = client.create_user_and_auth()
        req.userId = user_id
        response = client.raw.create_auction(req)
        with recon.validate(response) as validator:
            validator.status_code(400)

    requests = AuctionCreateRequest.invalid_requests()
    test_cases = []
    for req in requests:
        tc = recon.TestCase(
            "auction",
            f"create invalid auction: {req.reason}",
            lambda req=req.data: test_case(endpoints, req),
        )
        test_cases.append(tc)
    return test_cases


register_test_case_factory(invalid_create_test_cases)
