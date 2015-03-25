## How to run the jMeter tests?

You'll need:
* [jMeter](http://jmeter.apache.org/download_jmeter.cgi)
* Tomcat
* A MapFish Print 3 WAR (for example build one with `./gradlew war`)

Deploy the WAR and check that MapFish Print 3 works correctly. Then start jMeter and open
the file `jmeter-test-plan.jmx`. Make sure that the configuration in `MapFish Print Server Config`
matches your setup. There are several test cases available to run. The most complete is
`Create and Get Report`.

To run a test case, adjust the number of threads and loops, check the print request
in `Create Report Request` and press `Start`. You can check the
sent requests in `View Results Tree`.

For profiling hook up VisualVM with the Tomcat instance.
