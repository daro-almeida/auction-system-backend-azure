'use strict';

module.exports = {
    compare_arrays,
    random_object_key,
}

function compare_arrays(a1, a2) {
    if (a1.length != a2.length)
        return false;
    for (var i = 0; i < a1.length; ++i)
        if (a1[i] != a2[i])
            return false;
    return true;
}

function random_object_key(obj) {
    // https://stackoverflow.com/questions/2532218/pick-random-property-from-a-javascript-object
    var keys = Object.keys(obj);
    return keys[keys.length * Math.random() << 0];
}