import * as Scc from '../scc';
import { TestEnv, Tester, TestNamespace } from "../tester";

export function create(): TestNamespace {
    const ns = new TestNamespace("user");
    ns.register("create-user", createUserTest);
    ns.register("create-duplicate-user", createDuplicateUserTest);
    return ns;
}

async function createUserTest(env: TestEnv) {
    const requests = new Scc.Requests(env.endpoints);
    const response = await requests.createUser(Scc.randomCreateUserParams());
    env.validate(response, (response, assertions) => {
        assertions.status(response.status, 200);
        assertions.true(response.data !== undefined, "Response data is undefined");
        assertions.true(typeof response.data == 'number', "User ID is not a number");
        assertions.true((response.data as number) > 0, "User ID is empty");
    });
}

async function createDuplicateUserTest(env: TestEnv) {
    const requests = new Scc.Requests(env.endpoints);
    const params = Scc.randomCreateUserParams();
    env.validate(await requests.createUser(params), (response, assertions) => {
        assertions.status(response.status, 200);
    });
    env.validate(await requests.createUser(params), (response, assertions) => {
        assertions.status(response.status, 409);
    });
}