import scc
import recon
from scc.requests import QuestionCreateRequest, ReplyCreateRequest
from scc.clients import Client, RawClient
from scc.endpoints import Endpoints
from . import test_case


@test_case("question", "create question")
def create_question(endpoints: Endpoints):
    client = Client(endpoints)
    user_id, auction_id = client.create_user_and_auction()
    response = client.raw.create_question(auction_id, QuestionCreateRequest.random())
    with recon.validate(response) as validator:
        validator.status_code(200)
        if len(response.text) == 0:
            validator.fail("response body is empty")


@test_case("question", "create question with invalid user")
def create_question_with_invalid_user(endpoints: Endpoints):
    client1 = Client(endpoints)
    client2 = Client(endpoints)
    auction_id = client1.create_auction()
    response = client2.raw.create_question(auction_id, QuestionCreateRequest.random())
    with recon.validate(response) as validator:
        validator.status_code(401)


@test_case("question", "create reply")
def create_reply(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    client = Client(rclient)
    user_id, auction_id = client.create_user_and_auction()
    question_id = client.create_question(auction_id, QuestionCreateRequest.random())
    response = rclient.create_reply(
        auction_id, question_id, ReplyCreateRequest.random()
    )
    with recon.validate(response) as validator:
        validator.status_code(204)


@test_case("question", "create reply with invalid user")
def create_reply_with_invalid_user(endpoints: Endpoints):
    client1 = Client(endpoints)
    client2 = Client(endpoints)
    user_id, auction_id = client1.create_user_and_auction()
    question_id = client1.create_question(auction_id, QuestionCreateRequest.random())
    response = client2.raw.create_reply(
        auction_id, question_id, ReplyCreateRequest.random()
    )
    with recon.validate(response) as validator:
        validator.status_code(401)


@test_case("question", "create two replies")
def create_two_replies(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    client = Client(rclient)
    user_id, auction_id = client.create_user_and_auction()
    question_id = client.create_question(auction_id, QuestionCreateRequest.random())
    response = rclient.create_reply(
        auction_id, question_id, ReplyCreateRequest.random()
    )
    with recon.validate(response) as validator:
        validator.status_code(204)
    response = rclient.create_reply(
        auction_id, question_id, ReplyCreateRequest.random()
    )
    with recon.validate(response) as validator:
        validator.status_code(409)
