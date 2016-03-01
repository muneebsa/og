/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.http;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Operation;

/**
 * A utility class for working with http components
 * 
 * @since 1.0
 */
public class HttpUtil {
  public static final Set<Integer> VALID_STATUS_CODES =
      ContiguousSet.create(Range.closed(100, 599), DiscreteDomain.integers());
  public static final Set<Integer> SUCCESS_STATUS_CODES =
      ContiguousSet.create(Range.closed(200, 299), DiscreteDomain.integers());

  private HttpUtil() {}

  /**
   * Translates the provided operation into the corresponding method
   * 
   * @param operation the operation to convert
   * @return the translated operation instance
   */
  public static Method toMethod(final Operation operation) {
    checkNotNull(operation);
    switch (operation) {
      case WRITE:
      case OVERWRITE:
        return Method.PUT;
      case READ:
        return Method.GET;
      case METADATA:
        return Method.HEAD;
      case DELETE:
        return Method.DELETE;
      case LIST:
        return Method.GET;
      default:
        throw new IllegalArgumentException(String.format("Unrecognized operation [%s]", operation));
    }
  }
}
