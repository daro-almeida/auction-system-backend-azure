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
    parser.add_argument("filter", nargs="?", help="Regex to filter test cases")
    args = parser.parse_args()

    endpoints = Endpoints(args.host)
    recon.run(tests.create_test_cases(endpoints), filter=args.filter)


if __name__ == "__main__":
    main()
