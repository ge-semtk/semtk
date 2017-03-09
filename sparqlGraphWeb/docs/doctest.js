/**
 * Returns the sum of a and b
 * @param {Number} a
 * @param {Number} b
 * @param {Boolean} retArr If set to true, the function will return an array
 * @returns {Number|Array} Sum of a and b or an array that contains a, b and the sum of a and b.
 */
function sum(a, b, retArr) {
    if (retArr) {
        return [a, b, a + b];
    }
    return a + b;
}

/**
 * Another version of sum
 * @param a
 * @param b
 * @param retArr
 * @returns {Number|Array} Sum of a and b or an array that contains a, b and the sum of a and b.
 */
function sum2(a, b, retArr) {
    if (retArr) {
        return [a, b, a + b];
    }
    return a + b;
}


/**
 * A module representing a jacket.
 */
define('my/jacket', function() {
    /**
     * @constructor
     * @alias Jacket
     */
    var Jacket = function() {
        // ...
    };

    /** Zip up the jacket. */
    Jacket.prototype.zip = function() {
        // ...
    };

    return Jacket;
});