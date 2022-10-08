'use strict';

/***
 * Exported functions to be used in the testing scripts.
 */
module.exports = {
	uploadImageBody,
	processUploadReply,
	selectImageToDownload
}


const crypto = require('crypto')

var imagesIds = []
var images = []

// All endpoints starting with the following prefixes will be aggregated in the same for the statistics
var statsPrefix = [
	["/rest/media/", "GET"],
	["/rest/media", "POST"]
]

// Function used to compress statistics
global.myProcessEndpoint = function(str, method) {
	var i = 0;
	for (i = 0; i < statsPrefix.length; i++) {
		if (str.startsWith(statsPrefix[i][0]) && method == statsPrefix[i][1])
			return method + ":" + statsPrefix[i][0];
	}
	return method + ":" + str;
}

// Auxiliary function to select an element from an array
Array.prototype.sample = function() {
	return this[Math.floor(Math.random() * this.length)]
}

function loadData() {
	const image_count = 40;
	const image_size = 128 * 1024;
	for (var i = 0; i < image_count; ++i)
		images.push(crypto.randomBytes(image_size));
}

loadData();

/**
 * Sets the body to an image, when using images.
 */
function uploadImageBody(requestParams, context, ee, next) {
	requestParams.body = images.sample()
	return next()
}

/**
 * Process reply of the download of an image. 
 * Update the next image to read.
 */
function processUploadReply(requestParams, response, context, ee, next) {
	if (typeof response.body !== 'undefined' && response.body.length > 0) {
		imagesIds.push(response.body)
	}
	return next()
}

/**
 * Select an image to download.
 */
function selectImageToDownload(context, events, done) {
	if (imagesIds.length > 0) {
		context.vars.imageId = imagesIds.sample()
	} else {
		delete context.vars.imageId
	}
	return done()
}

