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
// Date: Jan 3, 2014
// ---------------------

package com.cleversafe.og.util.distribution;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.og.util.distribution.PoissonDistribution;

public class PoissonDistributionTest
{
   private static final double ERR = Math.pow(0.1, 6);

   @Test(expected = IllegalArgumentException.class)
   public void testNegativeMean()
   {
      new PoissonDistribution(-1.0);
   }

   @Test
   public void testZeroMean()
   {
      new PoissonDistribution(0.0);
   }

   @Test(expected = NullPointerException.class)
   public void testNullRandom()
   {
      new PoissonDistribution(10.0, null);
   }

   @Test
   public void testBasicPoissonDistribution()
   {
      final PoissonDistribution pd = new PoissonDistribution(10.0);
      Assert.assertEquals(10.0, pd.getAverage(), ERR);
      Assert.assertEquals(0.0, pd.getSpread(), ERR);
      pd.nextSample();
      pd.nextSample();
      pd.nextSample();
   }

   @Test
   public void testPoissonDistributionWithRandom()
   {
      final PoissonDistribution pd = new PoissonDistribution(10.0, new Random());
      pd.nextSample();
      pd.nextSample();
      pd.nextSample();
   }
}
