'use strict';

const { faker } = require('@faker-js/faker');
const crypto = require("crypto");
const utils = require("./utils");

module.exports = {
    on_user_create_complete,
    inject_random_user
}

function on_user_create_complete() { }

function inject_random_user(/**@type {Request} */ request, _context, _ee, next) {
    request.body = JSON.stringify({
        nickname: faker.internet.userName(),
        name: faker.name.fullName(),
        password: faker.internet.password(),
        imageBase64: crypto.randomBytes(64 * 1024).toString('base64'),
    });
    return next();
}