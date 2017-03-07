package org.mapfish.print.servlet;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter that adds CORS headers to every response.
 */
public final class CorsFilterHandlerInterceptor extends HandlerInterceptorAdapter {
    /**
     * Called before the reponse is built.
     *
     * @param request The request
     * @param response The response
     * @param handler The handler
     */@Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
        if (request.getHeader("Origin") != null && !response.containsHeader("Access-Control-Allow-Origin")) {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        return true;
    }
}
