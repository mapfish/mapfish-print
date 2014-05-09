/**
 * Pads a string with spaces on the specified side(s) up to the given length.
 * 
 * @param {String} s The string we will be padding.
 * @param {Number} len The length the returned padded string shall have.
 * @param {String} side The side where the padding will be added. Can be either
 *     `left`, `right` or `both`.
 * @returns {String} The padded string or the given string if an unrecognized
 *     side was passed.
 */
function pad(s, len, side) {
    var str = "" + s,
        length = parseInt(len, 10),
        spaces = (new Array(length + 1)).join(" ");
    switch (side.toLowerCase()) {
        case 'left':
            return (spaces + str).substr(-1*length);
            break;
        case 'right':
            return (str + spaces).substring(0, length);
            break;
        case 'both':
            var diff = length - str.length,
                leftLen = Math.floor(diff/2),
                rightLen = diff - leftLen;
            return pad('', leftLen, 'left') + str + pad('', rightLen, 'right');
    }
    // fallback for illegal side; returned the passed string directly
    return str;
}

/**
 * Pads a string with spaces on the left side up to the given length.
 * 
 * @param {String} s The string we will be padding.
 * @param {Number} len The length the returned padded string shall have.
 * @returns {String} The padded string.
 */
function padLeft(s, len) {
    return pad(s, len, 'left');
}

/**
 * Pads a string with spaces on the right side up to the given length.
 * 
 * @param {String} s The string we will be padding.
 * @param {Number} len The length the returned padded string shall have.
 * @returns {String} The padded string.
 */
function padRight(s, len) {
    return pad(s, len, 'right');
}

/**
 * Pads a string with spaces on both sides up to the given length.
 * 
 * @param {String} s The string we will be padding.
 * @param {Number} len The length the returned padded string shall have.
 * @returns {String} The padded string.
 */
function padBoth(s, len) {
    return pad(s, len, 'both');
}


exports.pad = pad;
exports.padLeft = padLeft;
exports.padRight = padRight;
exports.padBoth = padBoth;
