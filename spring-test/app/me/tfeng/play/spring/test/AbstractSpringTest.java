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

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import me.tfeng.play.spring.ApplicationContextHolder;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@ContextConfiguration({"classpath*:spring/**/*.xml", "classpath*:play-plugins/spring/**/*.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractSpringTest {

  private static final File TEMPORARY_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));

  @Autowired
  protected ConfigurableApplicationContext applicationContext;

  @After
  public void afterTest() throws IOException {
    TestLock.unlock();
  }

  @Before
  public void beforeTest() throws IOException, InterruptedException {
    TestLock.lock(lockFile());
    ApplicationContextHolder.set(applicationContext);
  }

  protected File lockFile() throws IOException {
    return new File(TEMPORARY_DIRECTORY, "play-test.lock");
  }
}
