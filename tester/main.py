import argparse

import tests.user
import tests.media
import tests.auction
import tests.question

import recon
import scc


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--host",
        default="http://localhost:8080",
        help="The base url of the host to test",
    )
    parser.add_argument("--groups", nargs="+", help="The groups to run")
    args = parser.parse_args()

    endpoints = scc.Endpoints(args.host)
    recon.run(
        groups=[
            tests.user.group(endpoints),
            tests.media.group(endpoints),
            tests.auction.group(endpoints),
            tests.question.group(endpoints),
        ],
        filters=args.groups,
    )


if __name__ == "__main__":
    main()
