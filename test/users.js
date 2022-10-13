'use strict';

const { faker } = require('@faker-js/faker');
const crypto = require("crypto");
const utils = require("./utils");

module.exports = {
    on_user_create_complete,
    inject_random_user,
    on_delete_user_complete,
    pick_uploaded_user
}

var UPLOADED_USERS = {}

function on_user_create_complete(/**@type {Response} */ request, /**@type {Response} */ response, _context, _ee, next) {
    if(response.status == 200 && response.body != null)
        UPLOADED_USERS[response.body] = request.body;
    return next();
}

function on_delete_user_complete(/**@type {Response} */ request, /**@type {Response} */ response, _context, _ee, next) {
    const userId = context.vars['userId'];
    if (response.body != null) {
    		const expected_body = UPLOADED_USERS[userId];
    		if (!utils.compare_arrays(expected_body, request.body))
    			throw new Error("Deleted an invalid user body");
            UPLOADED_USERS[userId] = null;
    	}
    return next();
}

function inject_random_user(/**@type {Request} */ request, _context, _ee, next) {
    request.body = JSON.stringify({
        nickname: faker.internet.userName(),
        name: faker.name.fullName(),
        password: faker.internet.password(),
        imageBase64: crypto.randomBytes(64 * 1024).toString('base64'),
    });
    return next();
}

function pick_uploaded_user(context, _ee, next){
    context.vars['userId'] = utils.random_object_key(UPLOADED_USERS);
    return next();
}