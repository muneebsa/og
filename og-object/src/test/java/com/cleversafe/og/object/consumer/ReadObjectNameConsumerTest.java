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
// Date: Jun 29, 2014
// ---------------------

package com.cleversafe.og.object.consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.consumer.ReadObjectNameConsumer;
import com.cleversafe.og.http.util.HttpUtil;
import com.cleversafe.og.object.ObjectName;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.object.manager.ObjectManagerException;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Method;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.operation.Response;
import com.cleversafe.og.util.consumer.Consumer;
import com.cleversafe.og.util.producer.ProducerException;

public class ReadObjectNameConsumerTest
{
   private ObjectManager mockObjectManager;
   private Map<Long, Request> pendingRequests;
   private List<Integer> statusCodes;
   private Request mockRequest;
   private Response mockResponse;

   @Before
   public void setBefore() throws URISyntaxException
   {
      this.mockObjectManager = mock(ObjectManager.class);
      this.statusCodes = HttpUtil.SUCCESS_STATUS_CODES;
      this.mockRequest = mock(Request.class);
      when(this.mockRequest.getUri()).thenReturn(
            new URI("http://192.168.8.1/soh/container/5c18be1057404792923dc487ca40f2370000"));
      this.mockResponse = mock(Response.class);
      when(this.mockResponse.getRequestId()).thenReturn(1L);

      this.pendingRequests = new HashMap<Long, Request>();
      this.pendingRequests.put(1L, this.mockRequest);
   }

   @Test(expected = NullPointerException.class)
   public void testNullObjectManager()
   {
      new ReadObjectNameConsumer(null, this.pendingRequests, this.statusCodes);
   }

   @Test(expected = NullPointerException.class)
   public void testNullPendingRequests()
   {
      new ReadObjectNameConsumer(this.mockObjectManager, null, this.statusCodes);
   }

   @Test(expected = NullPointerException.class)
   public void testNullStatusCodes()
   {
      new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests, null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testEmptyStatusCodes()
   {
      new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests,
            Collections.<Integer> emptyList());
   }

   @Test(expected = NullPointerException.class)
   public void testStatusCodesNullElement()
   {
      final List<Integer> sc = new ArrayList<Integer>();
      sc.add(null);
      new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests, sc);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testSmallStatusCode()
   {
      final List<Integer> sc = new ArrayList<Integer>();
      sc.add(99);
      new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests, sc);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testLargeStatusCode()
   {
      final List<Integer> sc = new ArrayList<Integer>();
      sc.add(600);
      new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests, sc);
   }

   @Test(expected = NullPointerException.class)
   public void testNullResponse()
   {
      new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests, this.statusCodes).consume(null);
   }

   @Test
   public void testSuccessfulRead()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.GET);
      when(this.mockRequest.getMetadata(Metadata.OBJECT_NAME)).thenReturn(
            "5c18be1057404792923dc487ca40f2370000");
      when(this.mockResponse.getStatusCode()).thenReturn(200);

      final Consumer<Response> c =
            new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests,
                  this.statusCodes);

      c.consume(this.mockResponse);
      verify(this.mockObjectManager).releaseNameFromRead(isA(ObjectName.class));
   }

   @Test
   public void testUnsuccessfulRead()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.GET);
      when(this.mockResponse.getStatusCode()).thenReturn(500);

      final Consumer<Response> c =
            new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests,
                  this.statusCodes);

      c.consume(this.mockResponse);
      verify(this.mockObjectManager, never()).releaseNameFromRead(isA(ObjectName.class));
   }

   @Test
   public void testOperationDoesNotMatchMethod()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.PUT);

      final Consumer<Response> c =
            new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests,
                  this.statusCodes);

      c.consume(this.mockResponse);
      verify(this.mockObjectManager, never()).releaseNameFromRead((isA(ObjectName.class)));
   }

   @Test(expected = ProducerException.class)
   public void testOperationManagerException()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.GET);
      when(this.mockResponse.getStatusCode()).thenReturn(200);
      doThrow(new ObjectManagerException()).when(this.mockObjectManager).releaseNameFromRead(
            any(ObjectName.class));

      final Consumer<Response> c =
            new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests,
                  this.statusCodes);

      c.consume(this.mockResponse);
   }

   @Test(expected = ProducerException.class)
   public void testNoPendingRequest()
   {
      when(this.mockRequest.getMethod()).thenReturn(Method.GET);
      when(this.mockResponse.getStatusCode()).thenReturn(200);
      // clear all pending requests
      this.pendingRequests.clear();

      new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests, this.statusCodes)
            .consume(this.mockResponse);
   }

   @Test(expected = ProducerException.class)
   public void testNoObject() throws URISyntaxException
   {
      when(this.mockRequest.getUri()).thenReturn(new URI("http://192.168.8.1/container"));
      when(this.mockRequest.getMethod()).thenReturn(Method.GET);
      when(this.mockResponse.getStatusCode()).thenReturn(200);

      new ReadObjectNameConsumer(this.mockObjectManager, this.pendingRequests, this.statusCodes)
            .consume(this.mockResponse);
   }
}