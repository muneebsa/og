//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Jun 16, 2014
// ---------------------

package com.cleversafe.oom.http.auth;

import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.util.Pair;

public interface HttpAuth
{
   public Pair<String, String> nextAuthorizationHeader(final Request request);
}
