/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.ibm.og.api.Body;
import com.ibm.og.api.Response;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ibm.og.api.RequestTimestamps;

/**
 * A defacto implementation of the {@code Response} interface
 * 
 * @since 1.0
 */
public class HttpResponse implements Response {
  private final int statusCode;
  private final Map<String, String> responseHeaders;
  private final Body body;
  private final Map<String, String> context;
  private final RequestTimestamps timestamps;

  private HttpResponse(final Builder builder) {
    this.statusCode = builder.statusCode;
    checkArgument(HttpUtil.VALID_STATUS_CODES.contains(this.statusCode),
        "statusCode must be a valid status code [%s]", this.statusCode);
    this.responseHeaders = ImmutableMap.copyOf(builder.responseHeaders);
    this.body = checkNotNull(builder.body);
    this.context = ImmutableMap.copyOf(builder.context);
    this.timestamps = builder.requestTimestamps;
  }

  @Override
  public int getStatusCode() {
    return this.statusCode;
  }

  @Override
  public Map<String, String> headers() {
    return this.responseHeaders;
  }

  @Override
  public Body getBody() {
    return this.body;
  }

  @Override
  public Map<String, String> getContext() {
    return this.context;
  }

  @Override
  public RequestTimestamps getRequestTimestamps() {
    return this.timestamps;
  }

  @Override
  public String toString() {
    return String.format(
        "HttpResponse [%n" + "statusCode=%s,%n" + "headers=%s%n" + "body=%s%n" + "context=%s%n]",
        this.statusCode, this.responseHeaders, this.body, this.context);
  }

  /**
   * An http response builder
   */
  public static class Builder {
    private int statusCode;
    private final Map<String, String> responseHeaders;
    private Body body;
    private final Map<String, String> context;
    private RequestTimestamps requestTimestamps;

    /**
     * Constructs a builder
     */
    public Builder() {
      this.responseHeaders = Maps.newLinkedHashMap();
      this.body = Bodies.none();
      this.context = Maps.newHashMap();
    }

    public Builder withStatusCode(final int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    /**
     * Configures a response header to include with this response
     * 
     * @param key a header key
     * @param value a header value
     * @return this builder
     */
    public Builder withHeader(final String key, final String value) {
      this.responseHeaders.put(key, value);
      return this;
    }

    /**
     * Configures a response body to include with this response
     * 
     * @param body a body
     * @return this builder
     */
    public Builder withBody(final Body body) {
      this.body = body;
      return this;
    }

    /**
     * Configures a context key to include with this response
     * 
     * @param key a context key
     * @param value a context value
     * @return this builder
     */
    public Builder withContext(final String key, final String value) {
      this.context.put(key, value);
      return this;
    }

    public Builder withRequestTimestamps(final RequestTimestamps timestamps) {
      this.requestTimestamps = timestamps;
      return this;
    }

    /**
     * Constructs an http response instance
     * 
     * @return an http response instance
     * @throws IllegalArgumentException if an invalid status code was configured with this builder
     * @throws NullPointerException if any null header keys or values were added to this builder
     */
    public HttpResponse build() {
      return new HttpResponse(this);
    }
  }
}
