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

package me.tfeng.play.avro.d2;

import me.tfeng.play.plugins.AvroD2Plugin;
import me.tfeng.play.spring.Startable;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class ZooKeeperClientStartable implements Startable {

  public static final String BEAN_NAME = "avro-d2-plugin.zookeeper-client";

  @Override
  public void onStart() throws Throwable {
    AvroD2Plugin.getInstance().connect();
    AvroD2Plugin.getInstance().startServers();
  }

  @Override
  public void onStop() throws Throwable {
    AvroD2Plugin.getInstance().stopServers();
  }
}
