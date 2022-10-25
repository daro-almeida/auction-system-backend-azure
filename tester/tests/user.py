import scc
import recon


def group(endpoints: scc.Endpoints):
    return recon.TestGroup(
        [
            create_user(endpoints),
            create_duplicate_user(endpoints),
            delete_user(endpoints),
            delete_missing_user(endpoints),
            authenticate(endpoints),
            authenticate_with_invalid_nickname(endpoints),
            authenticate_with_invalid_password(endpoints),
            *invalid_create_test_cases(endpoints),
        ]
    )


@recon.test_case("user", "create user")
def create_user(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    response = rclient.create_user(scc.UserCreateRequest.random())
    with recon.validate(response) as validator:
        validator.status_code(200)


@recon.test_case("user", "create duplicate user")
def create_duplicate_user(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    client = scc.Client(endpoints)

    request = scc.UserCreateRequest.random()
    client.create_user(request)

    response = rclient.create_user(request)
    with recon.validate(response) as validator:
        validator.status_code(409)


@recon.test_case("user", "delete user")
def delete_user(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    client = scc.Client(endpoints)

    request = scc.UserCreateRequest.random()
    user_id = client.create_user(request)

    response = rclient.delete_user(user_id)
    with recon.validate(response) as validator:
        validator.status_code(204)


@recon.test_case("user", "delete missing user")
def delete_missing_user(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    response = rclient.delete_user("missing")
    with recon.validate(response) as validator:
        validator.status_code(404)


@recon.test_case("user", "authenticate")
def authenticate(endpoints: scc.Endpoints):
    client = scc.Client(endpoints)
    user_create_request = scc.UserCreateRequest.random()
    user_id = client.create_user(user_create_request)
    response = client.raw.authenticate_user(
        scc.UserAuthenticateRequest(
            user_create_request.nickname, user_create_request.password
        )
    )

    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.cookie_exists("scc:session")


@recon.test_case("user", "authenticate with invalid nickname")
def authenticate_with_invalid_nickname(endpoints: scc.Endpoints):
    client = scc.Client(endpoints)
    response = client.raw.authenticate_user(
        scc.UserAuthenticateRequest("invalid", "password")
    )

    with recon.validate(response) as validator:
        validator.status_code(404)


@recon.test_case("user", "authenticate with invalid password")
def authenticate_with_invalid_password(endpoints: scc.Endpoints):
    client = scc.Client(endpoints)
    user_create_request = scc.UserCreateRequest.random()
    user_id = client.create_user(user_create_request)
    response = client.raw.authenticate_user(
        scc.UserAuthenticateRequest(user_create_request.nickname, "invalid")
    )

    with recon.validate(response) as validator:
        validator.status_code(401)


def invalid_create_test_cases(endpoints: scc.Endpoints) -> list[recon.TestCase]:
    def test_case(ep: scc.Endpoints, req: scc.UserCreateRequest):
        rclient = scc.RawClient(ep)
        response = rclient.create_user(req)
        with recon.validate(response) as validator:
            validator.status_code(400)

    requests = scc.UserCreateRequest.invalid_requests()
    test_cases = []
    # https://docs.python.org/3/faq/programming.html#why-do-lambdas-defined-in-a-loop-with-different-values-all-return-the-same-result
    for request in requests:
        test_cases.append(
            recon.TestCase(
                "user",
                f"create invalid user: {request.reason}",
                lambda req=request.data: test_case(endpoints, req),
            )
        )
    return test_cases
