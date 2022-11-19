import argparse
import recon
import tests
import datetime

from scc.endpoints import Endpoints
from scc.clients import Client
from scc.requests import *
from scc.responses import *


def testing(endpoints: Endpoints):
    client = Client(endpoints)
    user = client.create_user_and_auth()
    request = CreateAuctionRequest.random(user.id)
    request.endTime = datetime.datetime.now() + datetime.timedelta(minutes=1)
    response = client.create_auction(request)
    print(f"Created auction {response}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--host",
        default="http://localhost:8080",
        help="The base url of the host to test",
    )
    parser.add_argument("--testing", action="store_true")
    parser.add_argument("filter", nargs="?", help="Regex to filter test cases")
    args = parser.parse_args()

    endpoints = Endpoints(args.host)
    if args.testing:
        testing(endpoints)
    else:
        recon.run(tests.create_test_cases(endpoints), filter=args.filter)


if __name__ == "__main__":
    main()
