/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpRequest;
import com.cleversafe.og.http.Scheme;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A supplier of requests
 * 
 * @since 1.0
 */
public class RequestSupplier implements Supplier<Request> {
  private static final Joiner.MapJoiner PARAM_JOINER = Joiner.on('&').withKeyValueSeparator("=");
  private final Supplier<String> id;
  private final Method method;
  private final Scheme scheme;
  private final Supplier<String> host;
  private final Integer port;
  private final String uriRoot;
  private final Function<Map<String, String>, String> container;
  private final Function<Map<String, String>, String> object;
  private final Map<String, String> queryParameters;
  private final boolean trailingSlash;
  private final Map<String, Supplier<String>> headers;
  private final String username;
  private final String password;
  private final Supplier<Body> body;
  private final boolean virtualHost;

  /**
   * Creates an instance
   * 
   * @param id a supplier of ids to uniquely identify each request that is generated by this
   *        instance
   * @param method
   * @param scheme
   * @param host
   * @param port
   * @param uriRoot the base url part e.g. /soh/, /, /s3/
   * @param container
   * @param object
   * @param queryParameters static query parameters to all requests
   * @param trailingSlash whether or not to add a trailing slash to the url
   * @param headers headers to add to each request; header values may be dynamic
   * @param username
   * @param password
   * @param body a description of the request body to add to the request
   */
  public RequestSupplier(final Supplier<String> id, final Method method, final Scheme scheme,
      final Supplier<String> host, final Integer port, final String uriRoot,
      final Function<Map<String, String>, String> container,
      final Function<Map<String, String>, String> object,
      final Map<String, String> queryParameters, final boolean trailingSlash,
      final Map<String, Supplier<String>> headers, final String username, final String password,
      final Supplier<Body> body, final boolean virtualHost) {

    this.id = id;
    this.method = checkNotNull(method);
    this.scheme = checkNotNull(scheme);
    this.host = checkNotNull(host);
    this.port = port;
    this.uriRoot = uriRoot;
    this.container = checkNotNull(container);
    this.object = object;
    this.queryParameters = ImmutableMap.copyOf(queryParameters);
    this.trailingSlash = trailingSlash;
    this.headers = ImmutableMap.copyOf(headers);
    this.username = username;
    this.password = password;
    checkArgument((username != null && password != null) || (username == null && password == null),
        "username and password must both be not null or null [%s, %s]", username, password);
    if (this.username != null) {
      checkArgument(this.username.length() > 0, "username must not be empty string");
    }
    if (password != null) {
      checkArgument(password.length() > 0, "password must not be empty string");
    }
    this.body = body;
    this.virtualHost = virtualHost;
  }

  @Override
  public Request get() {
    final Map<String, String> context = Maps.newHashMap();
    final HttpRequest.Builder builder = new HttpRequest.Builder(this.method, getUrl(context));

    for (final Map.Entry<String, Supplier<String>> header : this.headers.entrySet()) {
      builder.withHeader(header.getKey(), header.getValue().get());
    }

    if (this.id != null) {
      builder.withHeader(Headers.X_OG_REQUEST_ID, this.id.get());
    }

    if (this.username != null && this.password != null) {
      builder.withHeader(Headers.X_OG_USERNAME, this.username);
      builder.withHeader(Headers.X_OG_PASSWORD, this.password);
    }

    for (final Map.Entry<String, String> entry : context.entrySet()) {
      builder.withHeader(entry.getKey(), entry.getValue());
    }

    if (this.body != null) {
      builder.withBody(this.body.get());
    }

    return builder.build();
  }

  private URI getUrl(final Map<String, String> context) {

    final StringBuilder s;
    if (this.virtualHost) {
      s =
          new StringBuilder().append(this.scheme).append("://")
              .append(this.container.apply(context)).append(".").append(this.host.get());
    } else {
      s = new StringBuilder().append(this.scheme).append("://").append(this.host.get());
    }
    appendPort(s);
    appendPath(s, context);
    appendTrailingSlash(s);
    appendQueryParams(s);

    try {
      return new URI(s.toString());
    } catch (final URISyntaxException e) {
      // Wrapping checked exception as unchecked because most callers will not be able to handle
      // it and I don't want to include URISyntaxException in the entire signature chain
      throw new IllegalArgumentException(e);
    }
  }

  private void appendPort(final StringBuilder s) {
    if (this.port != null) {
      s.append(":").append(this.port);
    }
  }

  private void appendPath(final StringBuilder s, final Map<String, String> context) {
    String objectName = null;
    if (this.object != null) {
      // FIXME must apply object first prior to container to populate context from object manager
      // for multi container (container suffix, object name)
      objectName = this.object.apply(context);
    }

    if (!this.virtualHost) {
      s.append("/");
      if (this.uriRoot != null) {
        s.append(this.uriRoot).append("/");
      }
      // Vault listing operation check to make sure container is not null.
      if (this.container.apply(context) != null) {
        s.append(this.container.apply(context));
      }
    }

    if (objectName != null) {
      s.append("/").append(objectName);
    }
  }

  private void appendTrailingSlash(final StringBuilder s) {
    if (this.trailingSlash) {
      s.append("/");
    }
  }

  private void appendQueryParams(final StringBuilder s) {
    final String queryParams = PARAM_JOINER.join(this.queryParameters);
    if (queryParams.length() > 0) {
      s.append("?").append(queryParams);
    }
  }

  @Override
  public String toString() {
    return String.format("RequestSupplier [%n" + "method=%s,%n" + "scheme=%s,%n" + "host=%s,%n"
        + "port=%s,%n" + "uriRoot=%s,%n" + "container=%s,%n" + "object=%s,%n"
        + "queryParameters=%s,%n" + "trailingSlash=%s,%n" + "headers=%s,%n" + "body=%s%n" + "]",
        this.method, this.scheme, this.host, this.port, this.uriRoot, this.container, this.object,
        this.queryParameters, this.trailingSlash, this.headers, this.body);
  }
}
