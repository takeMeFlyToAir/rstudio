/*
 * Stopwatch.java
 *
 * Copyright (C) 2009-17 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

public class Stopwatch
{
   /**
    * Track how long something takes, you know, like a Stopwatch.
    * @param debugLog Show timing via Debug.log
    */
   public Stopwatch(boolean debugLog)
   {
      debugLog_ = debugLog;
      reset();
   }

   public void reset()
   {
      startTime_ = System.currentTimeMillis();
   }

   public long mark(String label)
   {
      long stopTime = System.currentTimeMillis();
      if (debugLog_)
         Debug.log("[Stopwatch] " + label + ": " + (stopTime - startTime_) + " ms");
      return stopTime - startTime_;
   }

   private long startTime_;
   private boolean debugLog_;
}
