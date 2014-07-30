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
// Date: Jan 14, 2014
// ---------------------

package com.cleversafe.og.object;

/**
 * An identifying name for an object. Implementers should also override {@code toString} to provide
 * a string representation of this name.
 * 
 * @since 1.0
 */
public interface ObjectName extends Comparable<ObjectName>
{
   /**
    * Sets the name for this object, using the provided bytes
    * 
    * @param objectName
    *           the object name, in bytes
    */
   void setName(byte[] objectName);

   /**
    * Convert this instance's internal representation into bytes
    * 
    * @return this object name as bytes
    */
   byte[] toBytes();
}
