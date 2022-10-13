'use strict';

const { faker } = require('@faker-js/faker');
const crypto = require("crypto");

const media = {
    createRandomImage: function () {
        return crypto.randomBytes(64 * 1024);
    }
};

const user = {
    createRandomUser: function () {
        return {
            nickname: faker.internet.userName(),
            name: faker.name.fullName(),
            password: faker.internet.password(),
            imageBase64: crypto.randomBytes(64 * 1024).toString('base64'),
        };
    },
    setCreateUserHeaders: function (request) {
        request.headers = request.headers || {};
        request.headers['Content-Type'] = 'application/json';
        request.headers['Accept'] = 'application/json';
    }
};

const auction = {
    createRandomAuction: function (userId) {
        return {
            title: `${faker.commerce.productAdjective()} ${faker.commerce.productMaterial()} ${faker.commerce.product()}`,
            description: faker.lorem.paragraph(),
            userId: userId,
            endTime: faker.date.future(),
            minimumPrice: faker.commerce.price(),
            imageBase64: crypto.randomBytes(64 * 1024).toString('base64'),
        }
    },
    setCreateAuctionHeaders: function (request) {
        request.headers = request.headers || {};
        request.headers['Content-Type'] = 'application/json';
        request.headers['Accept'] = 'application/json';
    }
};

const rt = {
    contextAddRandomUser: function (context, events, done) {
        context.vars.user = user.createRandomUser();
        return done();
    },

    contextAddRandomAuction: function (context, events, done) {
        const user_id = context.vars.user_id;
        if (user_id == null)
            throw new Error("User id is not defined in context");
        context.vars.auction = auction.createRandomAuction(user_id);
        return done();
    },

    prepareDownloadImage: function (request, _context, _ee, next) {
        request.headers = request.headers || {};
        request.headers['Accept'] = 'application/octet-stream';
        return next();
    },

    prepareUploadRandomImage: function (request, _context, _ee, next) {
        const body = scc.media.createRandomImage();
        request.headers = request.headers || {};
        request.headers['Content-Type'] = 'application/octet-stream';
        request.headers['Content-Length'] = body.length;
        request.headers['Accept'] = 'application/json';
        request.body = body;
        return next();
    },

    prepareCreateUser: function (request, context, _ee, next) {
        const user = context.vars.user;
        if (user == null)
            throw new Error("User is not defined in context");
        user.setCreateUserHeaders(request)
        request.body = JSON.stringify(user);
        return next();
    },

    prepareCreateAuction: function (request, context, _ee, next) {
        const auction = context.vars.auction;
        if (auction == null)
            throw new Error("Auction is not defined in context");
        auction.setCreateAuctionHeaders(request)
        request.body = JSON.stringify(auction);
        return next();
    },

    extractImageIdFromBody: function (response, context, _ee, next) {
        context.vars['image_id'] = response.body;
        return next();
    }
};


module.exports = {
    media,
    user,
    auction,
};