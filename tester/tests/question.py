import scc
import recon


def group(endpoints: scc.Endpoints):
    return recon.TestGroup(
        [
            create_question(endpoints),
            create_question_with_invalid_user(endpoints),
            create_reply(endpoints),
            create_reply_with_invalid_user(endpoints),
            create_two_replies(endpoints),
        ]
    )


@recon.test_case("question", "create question")
def create_question(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    client = scc.Client(endpoints)
    user_id, auction_id = client.create_user_and_auction()
    response = rclient.create_question(
        auction_id, scc.QuestionCreateRequest.random(user_id)
    )
    with recon.validate(response) as validator:
        validator.status_code(200)
        if len(response.text) == 0:
            validator.fail("response body is empty")


@recon.test_case("question", "create question with invalid user")
def create_question_with_invalid_user(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    client = scc.Client(endpoints)
    auction_id = client.create_auction()
    response = rclient.create_question(
        auction_id, scc.QuestionCreateRequest.random("invalid")
    )
    with recon.validate(response) as validator:
        validator.status_code(404)


@recon.test_case("question", "create reply")
def create_reply(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    client = scc.Client(endpoints)
    user_id, auction_id = client.create_user_and_auction()
    question_id = client.create_question(
        auction_id, scc.QuestionCreateRequest.random(user_id)
    )
    response = rclient.create_reply(
        auction_id, question_id, scc.ReplyCreateRequest.random(user_id)
    )
    with recon.validate(response) as validator:
        validator.status_code(204)


@recon.test_case("question", "create reply with invalid user")
def create_reply_with_invalid_user(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    client = scc.Client(endpoints)
    user_id, auction_id = client.create_user_and_auction()
    question_id = client.create_question(
        auction_id, scc.QuestionCreateRequest.random(user_id)
    )
    response = rclient.create_reply(
        auction_id, question_id, scc.ReplyCreateRequest.random("invalid")
    )
    with recon.validate(response) as validator:
        validator.status_code(404)


@recon.test_case("question", "create two replies")
def create_two_replies(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    client = scc.Client(endpoints)
    user_id, auction_id = client.create_user_and_auction()
    question_id = client.create_question(
        auction_id, scc.QuestionCreateRequest.random(user_id)
    )
    response = rclient.create_reply(
        auction_id, question_id, scc.ReplyCreateRequest.random(user_id)
    )
    with recon.validate(response) as validator:
        validator.status_code(204)
    response = rclient.create_reply(
        auction_id, question_id, scc.ReplyCreateRequest.random(user_id)
    )
    with recon.validate(response) as validator:
        validator.status_code(409)
