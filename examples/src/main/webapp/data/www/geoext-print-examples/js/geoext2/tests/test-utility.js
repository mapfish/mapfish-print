/**
 * This script will export two methods to show and hide the <span> in the
 * testsuites run-tests.html which contains the <iframe> whose src will be the
 * single test-HTML-files.
 *
 * Use showTestIframe() to show the iframe and hideTestIframe() to hide it.
 *
 * For example, setSize does not work in iframes that are inside a hidden
 * container in FF and IE.  So, we make the hidden span element visible when
 * starting a test requiring it then hide it at the end.
 */
(function(global){
    var hiddenSpan = null,
        error = function(msg){
            global.Ext && global.Ext.log && global.Ext.log("Error: " + msg);
        };

    if (global &&
        global.parent &&
        global.parent.Test &&
        global.parent.Test.AnotherWay &&
        global.parent.Test.AnotherWay._g_test_iframe &&
        global.parent.Test.AnotherWay._g_test_iframe.frameElement &&
        global.parent.Test.AnotherWay._g_test_iframe.frameElement.parentNode) {
        hiddenSpan = global.parent.Test.AnotherWay._g_test_iframe.frameElement.parentNode;
    }

    // Export methods on the window object:
    global.showTestIframe = function(){
        if (hiddenSpan) {
            hiddenSpan.style.display = "";
        } else {
            error("Test iframe couldn't be found.");
        }
    };
    global.hideTestIframe = function(){
        if (hiddenSpan) {
            hiddenSpan.style.display = "none";
        } else {
            error("Test iframe couldn't be found.");
        }
    };
})(window);