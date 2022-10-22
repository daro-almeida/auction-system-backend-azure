import scc
import recon


def group(endpoints: scc.Endpoints):
    return recon.TestGroup(
        [
            create_auction(endpoints),
            create_auction_with_invalid_user(endpoints),
            *invalid_create_test_cases(endpoints),
        ]
    )


@recon.test_case("auction", "create auction")
def create_auction(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    client = scc.Client(endpoints)
    user_id = client.create_user(scc.UserCreateRequest.random())
    response = rclient.create_auction(scc.AuctionCreateRequest.random(user_id))
    with recon.validate(response) as validator:
        validator.status_code(200)


@recon.test_case("auction", "create auction with invalid user")
def create_auction_with_invalid_user(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    response = rclient.create_auction(scc.AuctionCreateRequest.random("invalid"))
    with recon.validate(response) as validator:
        validator.status_code(404)


def invalid_create_test_cases(endpoints: scc.Endpoints) -> list[recon.TestCase]:
    def test_case(ep: scc.Endpoints, req: scc.AuctionCreateRequest):
        rclient = scc.RawClient(ep)
        client = scc.Client(endpoints)
        user_id = client.create_user()
        req.userId = user_id
        response = rclient.create_auction(req)
        with recon.validate(response) as validator:
            validator.status_code(400)

    requests = scc.AuctionCreateRequest.invalid_requests(None)
    test_cases = []
    for req in requests:
        test_cases.append(
            recon.TestCase(
                "auction",
                f"create invalid auction: {req.reason}",
                lambda req=req.data: test_case(endpoints, req),
            )
        )
    return test_cases
