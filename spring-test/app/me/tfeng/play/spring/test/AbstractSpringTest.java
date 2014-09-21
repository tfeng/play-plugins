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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.AccessController;

import me.tfeng.play.spring.ApplicationContextHolder;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import sun.security.action.GetPropertyAction;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@ContextConfiguration({"classpath*:spring/**/*.xml", "classpath*:play-plugins/spring/**/*.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractSpringTest {

  private static final File TEMPORARY_DIRECTORY =
      new File(AccessController.doPrivileged(new GetPropertyAction("java.io.tmpdir")));

  @Autowired
  protected ConfigurableApplicationContext applicationContext;

  private FileLock testLock;

  @After
  public void afterTest() throws IOException {
    if (requireLocking()) {
      if (testLock != null) {
        testLock.release();
        testLock = null;
      }
    }
  }

  @Before
  public void beforeTest() throws IOException {
    if (requireLocking()) {
      File file = lockFile();
      if (file != null) {
        @SuppressWarnings("resource")
        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        testLock = channel.lock();
      }
    }

    ApplicationContextHolder.set(applicationContext);
  }

  protected File lockFile() throws IOException {
    return new File(TEMPORARY_DIRECTORY, "play-test.lock");
  }

  protected boolean requireLocking() {
    return true;
  }
}
