/**
 * Copyright 2014 Thomas Feng
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tfeng.play.spring.test;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public final class TestLock {

  private static final Object LOCAL_LOCK = new Object();

  private static final ALogger LOG = Logger.of(TestLock.class);

  private static FileLock fileLock;

  private static File lockFile;

  public static void lock(File file) {
    synchronized (LOCAL_LOCK) {
      try {
        if (fileLock != null) {
          throw new RuntimeException("Unexpected State!!!");
        }
        if (file != null) {
          @SuppressWarnings("resource")
          FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
          do {
            try {
              fileLock = channel.lock();
              break;
            } catch (OverlappingFileLockException e) {
              LOG.info("The lock is being held by anther test; waiting for 1 second before "
                  + "retrying ...");
              LOCAL_LOCK.wait(1000);
            }
          } while (true);
          lockFile = file;
        }
      } catch (Exception e) {
        throw new RuntimeException("Unable to lock " + file, e);
      }
    }
  }

  public static void unlock() {
    synchronized (LOCAL_LOCK) {
      try {
        if (fileLock != null) {
          fileLock.close();
          fileLock = null;
          lockFile = null;
        }
      } catch (Exception e) {
        throw new RuntimeException("Unable to unlock " + lockFile, e);
      }
    }
  }
}
