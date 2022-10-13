import axios from 'axios';
import { faker } from '@faker-js/faker';

import { Tester } from './tester';

import * as Scc from './scc';
import * as media from './tests/media';
import * as user from './tests/user';
import * as auction from './tests/auction';


async function main() {
    const endpoints = new Scc.Endpoints({
        baseUrl: 'http://localhost:8080',
    });
    const tester = new Tester(endpoints);

    media.register(tester);
    user.register(tester);
    auction.register(tester);

    tester.run();
}

main();