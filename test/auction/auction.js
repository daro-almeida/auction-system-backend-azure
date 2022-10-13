'use strict';

const scc = require('../scc');

module.exports = {
    contextAddRandomAuction,
    prepareCreateAuction,
};

function contextAddRandomAuction(context, events, done) {
    const user_id = context.vars.user_id;
    if (user_id == null)
        throw new Error("User id is not defined in context");
    context.vars.auction = scc.auction.createRandomAuction(user_id);
    return done();
}

function prepareCreateAuction(request, context, _ee, next) {
    const auction = context.vars.auction;
    if (auction == null)
        throw new Error("User is not defined in context");
    scc.user.setCreateUserHeaders(request)
    request.body = JSON.stringify(auction);
    return next();
}