/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.object;

import java.util.Set;

import com.cleversafe.og.api.Operation;

/**
 * A {@code ObjectNameConsumer} implementation which consumes object names for read operations
 * 
 * @since 1.0
 */
public class ReadObjectNameConsumer extends AbstractObjectNameConsumer {
  /**
   * Constructs an instance
   * 
   * @param objectManager the object manager for this instance to work with
   * @param statusCodes the status codes this instance should work with
   * @throws IllegalArgumentException if any status code in status codes is invalid
   */
  public ReadObjectNameConsumer(final ObjectManager objectManager, final Set<Integer> statusCodes) {
    super(objectManager, Operation.READ, statusCodes);
  }

  @Override
  protected void updateObjectManager(final ObjectMetadata objectName) {
    this.objectManager.getComplete(objectName);
  }

  @Override
  public String toString() {
    return "ReadObjectNameConsumer []";
  }
}
