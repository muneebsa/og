/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.api;


/**
 * An enum for describing an operation type
 * 
 * @since 1.0
 */
public enum Operation {
  ALL, WRITE, OVERWRITE, READ, METADATA, DELETE, LIST, CONTAINER_LIST, CONTAINER_CREATE,
  MULTIPART_WRITE, MULTIPART_WRITE_INITIATE, MULTIPART_WRITE_PART, MULTIPART_WRITE_COMPLETE, MULTIPART_WRITE_ABORT
}
