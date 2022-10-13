import { AxiosResponse } from 'axios';
import * as Scc from './scc';
import clc from 'cli-color';

export type TestRunner = (env: TestEnv) => Promise<void>;

type Response = AxiosResponse<any, any>;

class TestCase {
    readonly name: string;
    readonly runner: TestRunner;

    constructor(name: string, runner: TestRunner) {
        this.name = name;
        this.runner = runner;
    }
}

class AssertionError {
    readonly message: string;
    readonly response: Response;

    constructor(message: string, respone: Response) {
        this.message = message;
        this.response = respone;
    }
}

export class Assertions {
    private response: Response;

    constructor(response: Response) {
        this.response = response;
    }

    equals<T>(actual: T, expected: T, message?: string) {
        if (actual !== expected) {
            if (message === undefined) {
                throw new AssertionError(`Expected ${expected} but got ${actual}`, this.response);
            } else {
                throw new AssertionError(`${message}: Expected ${expected} but got ${actual}`, this.response);
            }
        }
    }

    true(actual: boolean, message?: string) {
        this.equals(actual, true, message);
    }

    false(actual: boolean, message?: string) {
        this.equals(actual, false, message);
    }

    status(actual: number, expected: number) {
        this.equals(actual, expected, 'Invalid status code');
    }
}

export class TestEnv {
    readonly endpoints: Scc.Endpoints

    constructor(endpoints: Scc.Endpoints) {
        this.endpoints = endpoints;
    }

    validate(response: AxiosResponse<any, any>, validator: (response: AxiosResponse<any, any>, assertions: Assertions) => void) {
        try {
            validator(response, new Assertions(response));
        } catch (e) {
            if (e instanceof AssertionError) {
                throw e;
            } else {
                throw new AssertionError(e + '', response);
            }
        }
    }
}

export class TestNamespace {
    readonly name: string;
    readonly tests: TestCase[];
    readonly namespaces: TestNamespace[];

    constructor(name: string) {
        this.name = name;
        this.tests = [];
        this.namespaces = [];
    }

    register(name: string, runner: TestRunner) {
        this.tests.push(new TestCase(name, runner));
    }

    registerNamespace(namespace: TestNamespace) {
        this.namespaces.push(namespace);
    }
}

export class Tester {
    private endpoints: Scc.Endpoints;
    private namespaces: Array<TestNamespace>;

    constructor(endpoints: Scc.Endpoints) {
        this.endpoints = endpoints;
        this.namespaces = [];
    }

    register(namespace: TestNamespace) {
        this.namespaces.push(namespace);
    }

    async run() {
        for (const namespace of this.namespaces) {
            await this.runNamespace(namespace);
        }
    }

    private async runNamespace(ns: TestNamespace) {
        for (const test of ns.tests) {
            await this.runTest(test);
        }
        for (const namespace of ns.namespaces) {
            await this.runNamespace(namespace);
        }
    }

    private async runTest(test: TestCase) {
        const env = new TestEnv(this.endpoints);
        try {
            await test.runner(env);
            console.log(`[${clc.green('OK')}] ${test.name}`);
        } catch (e) {
            if (e instanceof AssertionError) {
                console.log(`[${clc.red('FAIL')}] ${test.name}: ${e.message}`);
                console.log(e.response);
            } else {
                throw e;
            }
        }
    }
}