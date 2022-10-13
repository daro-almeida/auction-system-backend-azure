'use strict';

const scc = require('./scc');

module.exports = {
    contextAddRandomUser,
    contextAddRandomAuction,
    prepareDownloadImage,
    prepareUploadRandomImage,
    prepareCreateUser,
    prepareCreateAuction,
    extractImageIdFromBody,
    extractUserIdFromBody,
};

function contextAddRandomUser(context, events, done) {
    context.vars.user = scc.user.createRandomUser();
    return done();
}

function contextAddRandomAuction(context, events, done) {
    const user_id = context.vars.user_id;
    if (user_id == null)
        throw new Error("User id is not defined in context");
    context.vars.auction = scc.auction.createRandomAuction(user_id);
    return done();
}

function prepareDownloadImage(request, _context, _ee, next) {
    request.headers = request.headers || {};
    request.headers['Accept'] = 'application/octet-stream';
    return next();
}

function prepareUploadRandomImage(request, _context, _ee, next) {
    const body = scc.media.createRandomImage();
    request.headers = request.headers || {};
    request.headers['Content-Type'] = 'application/octet-stream';
    request.headers['Content-Length'] = body.length;
    request.headers['Accept'] = 'application/json';
    request.body = body;
    return next();
}

function prepareCreateUser(request, context, _ee, next) {
    const user = context.vars.user;
    if (user == null)
        throw new Error("User is not defined in context");
    scc.user.setCreateUserHeaders(request)
    request.body = JSON.stringify(user);
    return next();
}

function prepareCreateAuction(request, context, _ee, next) {
    const auction = context.vars.auction;
    if (auction == null)
        throw new Error("Auction is not defined in context");
    scc.auction.setCreateAuctionHeaders(request)
    request.body = JSON.stringify(auction);
    return next();
}

function extractImageIdFromBody(response, context, _ee, next) {
    context.vars['image_id'] = response.body;
    return next();
}

function extractUserIdFromBody(response, context, _ee, next) {
    console.log(`extractUserIdFromBody: response.body = ${response.body}`);
    context.vars['user_id'] = response.body;
    return next();
}