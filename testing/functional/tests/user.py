import scc
import recon
from scc.clients import Client, RawClient
from scc.endpoints import Endpoints
from scc.requests import UserAuthenticateRequest, UserCreateRequest
from . import register_test_case_factory, test_case


@test_case("user", "create user")
def create_user(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    response = rclient.create_user(UserCreateRequest.random())
    with recon.validate(response) as validator:
        validator.status_code(200)


@test_case("user", "authenticate user")
def authenticate_user(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    client = Client(rclient)
    user_create_request = UserCreateRequest.random()
    user_id = client.create_user(user_create_request)
    response = rclient.authenticate_user(
        UserAuthenticateRequest(user_id, user_create_request.password)
    )
    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.cookie_exists(scc.AUTH_COOKIE)


@test_case("user", "create duplicate user")
def create_duplicate_user(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    client = Client(rclient)

    request = UserCreateRequest.random()
    client.create_user(request)

    response = rclient.create_user(request)
    with recon.validate(response) as validator:
        validator.status_code(409)


@test_case("user", "delete user")
def delete_user(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    client = Client(rclient)

    request = UserCreateRequest.random()
    user_id = client.create_user_and_auth(request)

    response = rclient.delete_user(user_id)
    with recon.validate(response) as validator:
        validator.status_code(204)


@test_case("user", "delete missing user")
def delete_missing_user(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    response = rclient.delete_user("missing")
    with recon.validate(response) as validator:
        validator.status_code(401)


@test_case("user", "authenticate")
def authenticate(endpoints: Endpoints):
    client = Client(endpoints)
    user_create_request = UserCreateRequest.random()
    user_id = client.create_user(user_create_request)
    response = client.raw.authenticate_user(
        UserAuthenticateRequest(
            user_create_request.nickname, user_create_request.password
        )
    )

    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.cookie_exists("scc:session")


@test_case("user", "authenticate with invalid nickname")
def authenticate_with_invalid_nickname(endpoints: Endpoints):
    client = Client(endpoints)
    response = client.raw.authenticate_user(
        UserAuthenticateRequest("invalid", "password")
    )

    with recon.validate(response) as validator:
        validator.status_code(404)


@test_case("user", "authenticate with invalid password")
def authenticate_with_invalid_password(endpoints: Endpoints):
    client = Client(endpoints)
    user_create_request = UserCreateRequest.random()
    user_id = client.create_user(user_create_request)
    response = client.raw.authenticate_user(
        UserAuthenticateRequest(user_create_request.nickname, "invalid")
    )

    with recon.validate(response) as validator:
        validator.status_code(401)


def invalid_create_test_cases(endpoints: Endpoints) -> list[recon.TestCase]:
    def test_case(ep: Endpoints, req: UserCreateRequest):
        rclient = RawClient(ep)
        response = rclient.create_user(req)
        with recon.validate(response) as validator:
            validator.status_code(400)

    requests = UserCreateRequest.invalid_requests()
    # https://docs.python.org/3/faq/programming.html#why-do-lambdas-defined-in-a-loop-with-different-values-all-return-the-same-result
    test_cases = []
    for request in requests:
        tc = recon.TestCase(
            "user",
            f"create invalid user: {request.reason}",
            lambda req=request.data: test_case(endpoints, req),
        )
        test_cases.append(tc)
    return test_cases


register_test_case_factory(invalid_create_test_cases)
