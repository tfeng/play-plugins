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

import java.net.URL;

import org.apache.avro.Protocol;
import org.apache.zookeeper.ZooKeeper;

import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Server {

  private static final ALogger LOG = Logger.of(AvroD2Server.class);

  protected String nodePath;
  protected final Protocol protocol;
  protected final URL serverUrl;
  protected final ZooKeeper zk;

  public AvroD2Server(ZooKeeper zk, Protocol protocol, URL serverUrl) {
    this.zk = zk;
    this.protocol = protocol;
    this.serverUrl = serverUrl;
    register();
  }

  public void close() {
    try {
      if (nodePath != null) {
        LOG.info("Closing server " + serverUrl);
        zk.delete(nodePath, -1);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to close server at " + AvroD2Helper.getUri(protocol), e);
    }
  }

  public void register() {
    close();

    LOG.info("Registering server for " + AvroD2Helper.getUri(protocol) + " at " + serverUrl);
    try {
      nodePath = AvroD2Helper.createProtocolNode(zk, protocol, serverUrl);
    } catch (Exception e) {
      throw new RuntimeException("Unable to register server at " + AvroD2Helper.getUri(protocol),
          e);
    }
  }
}
