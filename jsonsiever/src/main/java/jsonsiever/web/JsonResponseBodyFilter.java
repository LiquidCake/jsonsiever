/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package jsonsiever.web;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jsonsiever.web.config.JsonFilteringSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Servlet filer to be used. See {@link jsonsiever.web.config.JsonFilteringConfig}
 */
public class JsonResponseBodyFilter extends OncePerRequestFilter {
   private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final JsonResponseFilterApplier jsonResponseFilterApplier;

    public JsonResponseBodyFilter(JsonResponseFilterApplier jsonResponseFilterApplier) {
        this.jsonResponseFilterApplier = jsonResponseFilterApplier;
    }

    protected boolean shouldNotFilterAsyncDispatch() {
        //we DO want to filter async dispatch to support async endpoints
        return false;
    }

    @Override
    public void destroy() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestPathKey = String.format("%s_%s",
                request.getMethod(),
                URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8));

        Optional<JsonFilteringSettings.Endpoint> endpointOpt =
                jsonResponseFilterApplier.findEndpointFilteringSettings(requestPathKey);

        if (endpointOpt.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Json filter configuration for request '{}' not found", requestPathKey);
            }

            chain.doFilter(request, response);

            return;
        }

        /* If we have json filtering settings for this endpoint
        - proceed with proxy response wrapper and later try to filter response body */
        
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        chain.doFilter(request, responseWrapper);

        if (request.isAsyncStarted()) {
            //if request is async - this filter will be called 2 times - this is a 'first' time,
            // and then on 'second' time request.isAsyncStarted() will return false.
            //At this 'first' step of async request processing (which is not writing data to user yet) - add callback
            // so our 'first' response wrapper instance will write its body to real response when async processing is complete.
            //Right now this 'first' wrapper instance has empty body but data will appear inside it after 'second' step of
            // async request processing finishes (data will be written by controller to 'second' wrapper instance, then we will
            // filter/write it to 'second' request output stream, and then data will appear also in 'first' wrapper)
            request.getAsyncContext().addListener(new AsyncListener() {
                public void onComplete(AsyncEvent asyncEvent) throws IOException {
                    responseWrapper.copyBodyToResponse();
                }

                public void onTimeout(AsyncEvent asyncEvent) throws IOException {
                }

                public void onError(AsyncEvent asyncEvent) throws IOException {
                }

                public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
                }
            });

            return;
        }

        //if this is a usual sync request, or a 'second' step of processing for async request - get response body (json expected)
        //from response wrapper and apply json filter to it. Then write filtered response body to real response object
        //NOTE: we could get inputStream from wrapper and write data directly to outputStream of request. But tests didnt show any RAM saving
        byte[] originalBody = responseWrapper.getContentAsByteArray();
        byte[] responseBody;

        if (originalBody.length > 0) {
            try {
                responseBody = jsonResponseFilterApplier.applyJsonFilterToResponseBody(
                        originalBody, endpointOpt.get(), request, requestPathKey);
            } catch (Exception e) {
                //in case of any error - default to original body
                responseBody = originalBody;
            }
        } else {
            responseBody = originalBody;
        }

        response.setContentLength(responseBody.length);
        response.getOutputStream().write(responseBody);
        response.flushBuffer();
    }
}
