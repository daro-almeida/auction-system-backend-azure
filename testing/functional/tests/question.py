import scc
import recon
from scc.clients import Client, RawClient
from scc.endpoints import Endpoints
from scc.requests import *
from scc.responses import *
from . import test_case


@test_case("question/create")
def create_question(endpoints: Endpoints):
    client = Client(endpoints)
    user, auction = client.create_user_and_auction()
    request = CreateQuestionRequest.random(user.id, auction.id)
    response = client.raw.create_question(auction.id, request)
    with recon.validate(response) as validator:
        validator.status_code(200)
        if len(response.text) == 0:
            validator.fail("response body is empty")
        question = QuestionDTO.parse_obj(response.json())
        validator.equals(question.authorId, user.id, "question user")
        validator.equals(question.text, request.text, "question content")
        validator.equals(question.reply, None, "question reply")


@test_case("question/create with invalid user")
def create_question_with_invalid_user(endpoints: Endpoints):
    client1 = Client(endpoints)
    client2 = Client(endpoints)
    user1, auction = client1.create_user_and_auction()
    response = client2.raw.create_question(
        auction.id, CreateQuestionRequest.random(user1.id, auction.id)
    )
    with recon.validate(response) as validator:
        validator.status_code(401)


@test_case("question/create reply")
def create_reply(endpoints: Endpoints):
    client = Client(endpoints)
    user, auction = client.create_user_and_auction()
    question = client.create_random_question(user.id, auction.id)
    request = CreateReplyRequest.random()
    response = client.raw.create_reply(auction.id, question.id, request)
    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.equals(response.text, question.id, "question id")


@test_case("question/create reply with invalid user")
def create_reply_with_invalid_user(endpoints: Endpoints):
    client1 = Client(endpoints)
    client2 = Client(endpoints)
    user, auction = client1.create_user_and_auction()
    question = client1.create_random_question(user.id, auction.id)
    response = client2.raw.create_reply(
        auction.id, question.id, CreateReplyRequest.random()
    )
    with recon.validate(response) as validator:
        validator.status_code(401)


@test_case("question/create two replies")
def create_two_replies(endpoints: Endpoints):
    client = Client(endpoints)
    user, auction = client.create_user_and_auction()
    question = client.create_random_question(user.id, auction.id)
    request = CreateReplyRequest.random()
    response = client.raw.create_reply(auction.id, question.id, request)
    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.equals(response.text, question.id, "question id")

    request = CreateReplyRequest.random()
    response = client.raw.create_reply(auction.id, question.id, request)
    with recon.validate(response) as validator:
        validator.status_code(409)
