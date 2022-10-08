'use strict';

const crypto = require("crypto");
const utils = require("./utils");

module.exports = {
	on_upload_complete,
	on_download_complete,
	inject_image_body,
	pick_uploaded_image,
}

var UPLOADED_IMAGES = {}

function on_upload_complete(/**@type {Response} */ request, /**@type {Response} */ response, _context, _ee, next) {
	if (response.status == 200 && response.body != null)
		UPLOADED_IMAGES[response.body] = request.body;
	return next();
}

function on_download_complete(/**@type {Response} */ request, /**@type {Response} */ response, context, _ee, next) {
	const image_id = context.vars['image_id'];
	if (response.body != null) {
		const expected_body = UPLOADED_IMAGES[image_id];
		if (!utils.compare_arrays(expected_body, request.body))
			throw new Error("Received an invalid image body");

	}
	return next();
}

function inject_image_body(/**@type {Request} */ request, _context, _ee, next) {
	const body = crypto.randomBytes(64 * 1024);
	request.body = body;
	return next();
}

function pick_uploaded_image(context, _ee, next) {
	context.vars['image_id'] = utils.random_object_key(UPLOADED_IMAGES)
	return next();
}
