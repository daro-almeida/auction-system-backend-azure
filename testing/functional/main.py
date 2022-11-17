import argparse
import recon
import tests
from scc.endpoints import Endpoints


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--host",
        default="http://localhost:8080",
        help="The base url of the host to test",
    )
    parser.add_argument("--groups", nargs="+", help="The groups to run")
    args = parser.parse_args()

    endpoints = Endpoints(args.host)
    recon.run(
        tests=tests.create_test_cases(endpoints),
        filters=args.groups,
    )


if __name__ == "__main__":
    main()
