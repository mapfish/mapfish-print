var fs = require('fs'),
    system = require('system'),
    dash = fs.separator,
    currentFile = system.args[4], // the full path to run-testsuite.js
    curFileParts = fs.absolute(currentFile).split(dash);

// remove filename 'run-testsuite.js'
curFileParts.pop();
// remove containing folder 'headless' 
curFileParts.pop();

var basePath = curFileParts.join(dash) + dash;
var modulePath = basePath  + 'headless' + dash + 'modules' + dash;

// the html file with the links
var tawListOfTestsFile = basePath + 'list-tests.html';

var strUtil = require(modulePath + 'strUtil'),
    padLeft = strUtil.padLeft,
    padRight = strUtil.padRight;

var tawWrapper = require(modulePath + 'tawWrapper'),
    getTestFiles = tawWrapper.getTestFiles,
    getTestFunctions = tawWrapper.getTestFunctions,
    runOneTestFunc = tawWrapper.runOneTestFunc;


// the variables we'll fill during the first phase and iterate over in the
// second phase
var links = [];
var funcCnt = 0;
var results = [];
var total = 0;



// Open the TAW-list of urls
casper.start(tawListOfTestsFile);

casper.then(function() {
    // get all testfiles
    links = this.evaluate(getTestFiles);
});

casper.then(function() {
    links.forEach(function(link) {
        var funcs;
        casper.thenOpen(basePath + link, function(){
            funcs = this.evaluate(getTestFunctions);
        });
        casper.then(function() {
            funcCnt += funcs.length;
            funcs.forEach(function(func){
                var oneFuncResults = this.evaluate(runOneTestFunc, func, link) || [],
                    oneFuncLen = oneFuncResults.length;
                total += oneFuncLen;
                results.push(oneFuncResults);
                this.echo(
                    padRight(link, 30) + " " + oneFuncLen +
                    " test" + (oneFuncLen !== 1 ? 's': '') +
                    " in "  + func + "(t)"
                );
            }, this);
        });
        
    });
});

casper.then(function(){
    var realTestNum = total - funcCnt;
    casper.test.begin('Excuting a total of ' + realTestNum + ' Test.Anotherway tests', realTestNum, function(test){
        var cnt = 0;
        results.forEach(function(oneFuncResults) {
            oneFuncResults.forEach(function(result){
                if (result.info) {
                    test.info(result.info);
                } else {
                    var message = padLeft((++cnt), 4) + ") " + result.msg;
                    if (result.skip) {
                        test.skip(1, message)
                    } else {
                        test.assert(result.pass, message);
                    }
                }
            });
        });
        test.done();
    });
    
});

casper.run();