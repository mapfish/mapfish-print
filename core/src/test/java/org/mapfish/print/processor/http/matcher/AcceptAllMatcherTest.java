package org.mapfish.print.processor.http.matcher;

import org.junit.Test;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static org.mapfish.print.processor.http.matcher.MatcherTestUtils.assertMatch;

public class AcceptAllMatcherTest {

    @Test
    public void testAccepts() throws Exception {
        AcceptAllMatcher matcher = new AcceptAllMatcher();
        assertMatch(matcher, true, new URI("http://localhost/print-servlet"), HttpMethod.GET);
        assertMatch(matcher, true, new URI("http://www.camptocamp.com"), HttpMethod.GET);
        assertMatch(matcher, true, new URI("http://www.camptocamp.com"), HttpMethod.POST);
    }
}
