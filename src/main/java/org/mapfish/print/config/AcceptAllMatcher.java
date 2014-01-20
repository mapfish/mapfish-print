package org.mapfish.print.config;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;

/**
* Created by Jesse on 1/20/14.
*/
public class AcceptAllMatcher extends HostMatcher {
    @Override
    public boolean validate(URI uri) throws UnknownHostException, SocketException, MalformedURLException {
        return true;
    }

    @Override
    public String toString() {
        return "Accept All";
    }

}
