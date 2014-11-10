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
import java.util.concurrent.TimeUnit;

import me.tfeng.play.plugins.AvroD2Plugin;
import me.tfeng.play.spring.ExtendedStartable;

import org.apache.avro.Protocol;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Server implements ExtendedStartable, Watcher {

  private static final ALogger LOG = Logger.of(AvroD2Server.class);

  protected volatile String nodePath;
  protected final Protocol protocol;
  protected final URL url;

  public AvroD2Server(Protocol protocol, URL url) {
    this.protocol = protocol;
    this.url = url;
  }

  @Override
  public void afterStart() throws Throwable {
    register();
  }

  @Override
  public void afterStop() throws Throwable {
  }

  @Override
  public void beforeStart() throws Throwable {
  }

  @Override
  public void beforeStop() throws Throwable {
    close();
  }

  public synchronized void close() throws InterruptedException, KeeperException {
    String path = nodePath;
    if (path != null) {
      LOG.info("Closing server for " + protocol.getName() + " at " + url);
      try {
        AvroD2Plugin.getInstance().getZooKeeper().delete(path, -1);
      } catch (NoNodeException e) {
        // Ignore.
      }
      nodePath = null;
    }
  }

  public Protocol getProtocol() {
    return protocol;
  }

  public URL getUrl() {
    return url;
  }

  @Override
  public void onStart() throws Throwable {
  }

  @Override
  public void onStop() throws Throwable {
  }

  @Override
  public void process(WatchedEvent event) {
    if (event.getType() == EventType.NodeDeleted && event.getPath().equals(nodePath)
        || event.getType() == EventType.None && event.getState() == KeeperState.SyncConnected) {
      // If the node is unexpectedly deleted or if ZooKeeper connection is restored, register the
      // server again.
      register();
    }
  }

  public synchronized void register() {
    try {
      close();
      LOG.info("Registering server for " + protocol.getName() + " at " + url);
      ZooKeeper zk = AvroD2Plugin.getInstance().getZooKeeper();
      nodePath = AvroD2Helper.createProtocolNode(zk, protocol, url);
      zk.getData(nodePath, this, null);
    } catch (Exception e) {
      LOG.warn("Unable to register server for " + protocol.getName() + "; retry later", e);
      AvroD2Plugin.getInstance().getScheduler().schedule(() -> register(),
          AvroD2Plugin.getInstance().getServerRegisterRetryDelay(), TimeUnit.MILLISECONDS);
    }
  }
}
