import scc
import recon
from scc.clients import Client, RawClient
from scc.endpoints import Endpoints
from scc.requests import *
from scc.responses import *
from . import register_test_case_factory, test_case

# TODO:
#   + test get user
#   + test with media id


@test_case("user/create user")
def create_user(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    request = CreateUserRequest.random()
    response = rclient.create_user(request)
    with recon.validate(response) as validator:
        validator.status_code(200)
        user = CreateUserResponse.parse_obj(response.json())
        validator.equals(user.id, request.id, "user id")
        validator.equals(user.name, request.name, "user name")
        validator.equals(user.pwd, request.pwd, "user pwd")


@test_case("user/authenticate user")
def authenticate_user(endpoints: Endpoints):
    client = Client(endpoints)
    user = client.create_user()
    request = AuthenticateUserRequest(user=user.id, pwd=user.pwd)
    response = client.raw.authenticate_user(request)
    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.cookie_exists(scc.AUTH_COOKIE)


@test_case("user/create duplicate user")
def create_duplicate_user(endpoints: Endpoints):
    client = Client(endpoints)
    request = CreateUserRequest.random()
    client.create_user(request)
    response = client.raw.create_user(request)
    with recon.validate(response) as validator:
        validator.status_code(409)


@test_case("user/get user")
def get_user(endpoints: Endpoints):
    client = Client(endpoints)
    create_user = client.create_user()
    response = client.raw.get_user(create_user.id)
    with recon.validate(response) as validator:
        validator.status_code(200)
        user = UserDAO.parse_obj(response.json())
        validator.equals(create_user.id, user.id, "user id")
        validator.equals(create_user.name, user.name, "user name")


@test_case("user/delete user")
def delete_user(endpoints: Endpoints):
    client = Client(endpoints)
    user = client.create_user_and_auth()
    response = client.raw.delete_user(user.id)
    with recon.validate(response) as validator:
        validator.status_code(204)


@test_case("user/delete missing user")
def delete_missing_user(endpoints: Endpoints):
    client = Client(endpoints)
    response = client.raw.delete_user("missing")
    with recon.validate(response) as validator:
        validator.status_code(401)


@test_case("user/authenticate")
def authenticate(endpoints: Endpoints):
    client = Client(endpoints)
    user = client.create_user()
    request = AuthenticateUserRequest(user=user.id, pwd=user.pwd)
    response = client.raw.authenticate_user(request)
    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.cookie_exists(scc.AUTH_COOKIE)


@test_case("user/authenticate with invalid id")
def authenticate_with_invalid_nickname(endpoints: Endpoints):
    client = Client(endpoints)
    request = AuthenticateUserRequest(user="invalid", pwd="invalid")
    response = client.raw.authenticate_user(request)
    with recon.validate(response) as validator:
        validator.status_code(404)


@test_case("user/authenticate with invalid password")
def authenticate_with_invalid_password(endpoints: Endpoints):
    client = Client(endpoints)
    user = client.create_user()
    request = AuthenticateUserRequest(user=user.id, pwd="invalid")
    response = client.raw.authenticate_user(request)
    with recon.validate(response) as validator:
        validator.status_code(401)


@test_case("user/create invalid", invalidators=CreateUserRequest.invalidators())
def create_invalid_user(
    endpoints: Endpoints, invalidator: Invalidator[CreateUserRequest]
):
    client = Client(endpoints)
    request = invalidator(CreateUserRequest.random())
    response = client.raw.create_user(request)
    with recon.validate(response) as validator:
        validator.status_code(400)
