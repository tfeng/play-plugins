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
import java.util.concurrent.Semaphore;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public final class TestLock {

  private static FileLock fileLock;

  private static File lockFile;

  private static final Semaphore SEMAPHORE = new Semaphore(1);

  public static void lock(File file) {
    try {
      SEMAPHORE.acquire();
      if (fileLock != null) {
        throw new RuntimeException("Unexpected State!!!");
      }
      if (file != null) {
        @SuppressWarnings("resource")
        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        fileLock = channel.lock(0, 0, false);
        lockFile = file;
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to lock " + file, e);
    }
  }

  public static void unlock() {
    try {
      if (fileLock != null) {
        fileLock.close();
        fileLock = null;
        lockFile = null;
      }
      SEMAPHORE.release();
    } catch (Exception e) {
      throw new RuntimeException("Unable to unlock " + lockFile, e);
    }
  }
}
