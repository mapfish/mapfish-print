/**
 * When executed in the context of the 'list-tests.html' file, this method will
 * return an array of all the test files referenced in the list.
 *
 * This method assumes a list-tests.html layout of:
 *
 *     <ul id="testlist">
 *         <li>Action.html</li>
 *         <!-- ... -->
 *     </ul>
 *
 * @returns {String[]} The names of the referenced test files.
 */
function getTestFiles() {
    var links = document.querySelectorAll('li');
    return Array.prototype.map.call(links, function(e) {
        return e.innerHTML;
    });
}

/**
 * When executed in the context of a test file, this method will return an array
 * of the names of all globally defined functions with their names starting with
 * 'test_', these are the single functions that Test.Anotherway would be
 * executing.
 *
 * @returns {String[]}
 */
function getTestFunctions() {
    var funcs = [],
        isFunc = function (obj) {
            return Object.prototype.toString.call(obj) === '[object Function]';
        };
    for ( var key in window ) {
        if ( /test_/.test(key) && isFunc(window[key]) ) {
            funcs.push(key);
        }
    }
    return funcs;
}

/**
 * Runs the function with the given name in the context of the file it was
 * defined in. Expects to be called in that context. Will return an array of
 * objects with details about the tests in the functions: 
 * 
 *   * The first object has one member `info` with the passed over function name
 *     and the name of the file.
 *   * All other objects returned are the results of a single test. All contain
 *     a member `msg` with a printable message, and then either the key `pass`
 *     or the key `skip`:
 *     * `pass` will be there if we could run the function and could gather a
 *       test result. If `pass` is `true` the test passed, if the test failed,
 *       `pass` will be `false`.
 *     * `skip` will be there if we detected a method call that we currently
 *       cannot emulate. This will be the case for `t.delay_call` and
 *       `t.wait_result`.
 * 
 * @param {String} name The name of a test function (usually gathered by
 *     #getTestFunctions). 
 * @param {String} filename The filename that the function is defined in. Mainly
 *     there to have a better `info` member.
 * @returns {Object[]} An array with a meta-object (for informational purposes)
 *     and one object with the test results for every executed or skipped test.
 */
function runOneTestFunc(name, filename) {
    var results = [
            {info: filename + " " + name + "(t):"}
        ],
        toString = Object.prototype.toString,
        isArr = function(o) {
            return toString.call(o) === '[object Array]';
        },
        isObj = function(o) {
            return toString.call(o) === '[object Object]';
        },
        eqFunc = function(a, b) {
            if (a === null && b === null) {
                return true;
            }
            if (toString.call(a) !== toString.call(b)) {
                return false;
            }
            if (isArr(a)) {
                if (a.length !== b.length) {
                    return false;
                }
                for (var i = 0; i < a.length; i++) {
                    if (!eqFunc(a[i], b[i])) {
                        return false;
                    }
                }
                return true;
            }
            if (isObj(a)) {
                for (var k in a) {
                    if (!eqFunc(a[k], b[k])) {
                        return false;
                    }
                }
                // could be optimized
                for (var kk in b) {
                    if (!eqFunc(a[kk], b[kk])) {
                        return false;
                    }
                }
                return true;
            }
            return a === b;
        },
        debugInfo = function(mockMethod, testName, filename, other) {
            return [
                " [",
                mockMethod + ", ",
                testName + "(t), ",
                filename,
                (other ? other : ''),
                "]"
            ].join("");
        },
        mock = {
            plan: function() {},
            ok: function(cond, msg) {
                if (!cond) {
                    results.push({
                        pass: false,
                        msg: msg + debugInfo("t.ok", name, filename)
                    })
                } else {
                    results.push({pass: true, msg: msg});
                }
            },
            eq: function(got, exp, msg) {
                //if (got !== exp) {
                if(!eqFunc(got, exp)) {
                    results.push({
                        pass: false,
                        msg: msg +
                            debugInfo(
                                "t.eq", name, filename,
                                "exp: " + exp + ", got: " + got
                            )
                    });
                } else {
                    results.push({pass: true, msg: msg});
                }
            },
            fail: function(msg) {
                results.push({
                    pass: false,
                    msg: msg + debugInfo("t.fail", name, filename)
                });
            },
            delay_call : function(s, fn) {
                // TODO
                results.push({
                    skip: true,
                    msg: "not supported" + debugInfo("t.delay_call", name, filename)
                });
            },
            wait_result: function(s) {
                // TODO
                results.push({
                    skip: true,
                    msg: "not supported" + debugInfo("t.wait_result", name, filename)
                });
            }
        };
    if (window[name]) {
        try {
            window[name].call(this, mock);
        } catch (e) {
            results = [{
                pass: false,
                msg: "Caught exception: " + debugInfo("calling method", name, filename, e)
            }];
        }
    }
    return results;
}


exports.getTestFiles = getTestFiles;
exports.getTestFunctions = getTestFunctions;
exports.runOneTestFunc = runOneTestFunc;
