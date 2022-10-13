'use strict';

const crypto = require("crypto");
const scc = require("../scc");

module.exports = {
    prepareUploadRandomImage,
    extractImageIdFromBody,
    prepareDownloadImage,
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

function extractImageIdFromBody(/**@type {Response} */ response, context, _ee, next) {
    context.vars['image_id'] = response.body;
    return next();
}