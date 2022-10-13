import * as Scc from '../scc';
import { TestEnv, Tester } from "../tester";

export function register(tester: Tester) {
    tester.register("auctions/create-auction", createAuctionTest);
}

async function createAuctionTest(env: TestEnv) {
    const requests = new Scc.Requests(env.endpoints);
    const client = new Scc.Client(env.endpoints);

    const clientId = await client.createUser(Scc.randomCreateUserParams());
    const response = await requests.createAuction(Scc.randomCreateAuctionParams(clientId));
    env.validate(response, (response, assertions) => {
        assertions.status(response.status, 200);
        assertions.true(response.data !== undefined, "Response data is undefined");
        assertions.true(typeof response.data == 'number', "Auction ID is not a number");
        assertions.true((response.data as number) > 0, "Auction ID is empty");
    });
}