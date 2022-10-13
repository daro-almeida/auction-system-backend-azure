'use strict';

const scc = require('../scc');

module.exports = {
    contextAddRandomUser,
    prepareCreateUser,
};

function contextAddRandomUser(context, events, done) {
    context.vars.user = scc.user.createRandomUser();
    return done();
}

function prepareCreateUser(request, context, _ee, next) {
    const user = context.vars.user;
    if (user == null)
        throw new Error("User is not defined in context");
    scc.user.setCreateUserHeaders(request)
    request.body = JSON.stringify(user);
    return next();
}
