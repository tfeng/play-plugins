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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.IpcRequestor;
import org.apache.avro.specific.SpecificData;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import me.tfeng.play.avro.AvroHelper;
import me.tfeng.play.avro.PostRequestPreparerChain;
import me.tfeng.play.http.PostRequestPreparer;
import me.tfeng.play.plugins.AvroD2Plugin;
import me.tfeng.play.utils.Constants;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Client implements Watcher, InvocationHandler {

  private static final ALogger LOG = Logger.of(AvroD2Client.class);

  private final SpecificData data;
  private boolean isGeneric;
  private boolean isVersionRegistered;
  private volatile int lastIndex = -1;
  private volatile PostRequestPreparerChain postRequestPreparerChain =
      new PostRequestPreparerChain();
  private final Protocol protocol;
  private volatile IpcRequestor requestor;
  private final List<URL> serverUrls = new ArrayList<>();

  public AvroD2Client(Class<?> interfaceClass) {
    this(interfaceClass, new SpecificData(interfaceClass.getClassLoader()));
  }

  public AvroD2Client(Class<?> interfaceClass, SpecificData data) {
    this.data = data;
    protocol = AvroHelper.getProtocol(interfaceClass);
  }

  public AvroD2Client(Protocol protocol) {
    this(protocol, SpecificData.get());
  }

  public AvroD2Client(Protocol protocol, SpecificData data) {
    this.data = data;
    this.protocol = protocol;
  }

  public void addPostRequestPreparer(PostRequestPreparer postRequestPreparer) {
    postRequestPreparerChain.add(postRequestPreparer);
    if (requestor != null) {
      requestor.addPostRequestPreparer(postRequestPreparer);
    }
  }

  public URL getNextServerUrl() {
    synchronized(serverUrls) {
      if (serverUrls.isEmpty()) {
        throw new RuntimeException("No server is found for " + protocol.getName());
      } else {
        lastIndex = (lastIndex + 1) % serverUrls.size();
        return serverUrls.get(lastIndex);
      }
    }
  }

  public Protocol getProtocol() {
    return protocol;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    setupRequest();
    return requestor.invoke(proxy, method, args);
  }

  public boolean isGeneric() {
    return isGeneric;
  }

  @Override
  public void process(WatchedEvent event) {
    refresh();
  }

  public void refresh() {
    ZooKeeper zk = AvroD2Plugin.getInstance().getZooKeeper();
    if (zk == null) {
      LOG.warn("ZooKeeper is not configured; retry listing servers later");
      scheduleRefresh();
      return;
    }

    List<String> children;
    String path = AvroD2Helper.getServersZkPath(protocol);
    try {
      children = zk.getChildren(path, this);
    } catch (Exception e) {
      LOG.warn("Unable to list servers for " + protocol.getName() + "; retry later", e);
      scheduleRefresh();
      return;
    }

    synchronized(serverUrls) {
      serverUrls.clear();
      for (String child : children) {
        String childPath = path + "/" + child;
        try {
          byte[] data = zk.getData(childPath, false, null);
          String serverUrl = new String(data, Constants.UTF8);
          serverUrls.add(new URL(serverUrl));
        } catch (Exception e) {
          LOG.warn("Unable to get server URL from node " + childPath, e);
        }
      }

      if (serverUrls.isEmpty()) {
        LOG.warn("Unable to get any server URL for protocol " + protocol.getName()
            + "; retry later");
        scheduleRefresh();
      }
    }
  }

  public void removePostRequestPreparer(PostRequestPreparer postRequestPreparer) {
    postRequestPreparerChain.remove(postRequestPreparer);
    if (requestor != null) {
      requestor.removePostRequestPreparer(postRequestPreparer);
    }
  }

  public Promise<Object> request(String message, Object[] request) throws Exception {
    setupRequest();
    return requestor.request(message, request);
  }

  public void setGeneric(boolean isGeneric) {
    this.isGeneric = isGeneric;
  }

  private void scheduleRefresh() {
    AvroD2Plugin.getInstance().getScheduler().schedule(this::refresh,
        AvroD2Plugin.getInstance().getClientRefreshRetryDelay(), TimeUnit.MILLISECONDS);
  }

  private synchronized void setupRequest() throws IOException, KeeperException, InterruptedException {
    if (requestor == null) {
      refresh();
      requestor = new IpcRequestor(protocol, new AvroD2Transceiver(this), data);
      requestor.setGeneric(isGeneric);
      requestor.addPostRequestPreparer(postRequestPreparerChain);
    }
    if (!isVersionRegistered) {
      AvroD2Helper.createVersionNode(AvroD2Plugin.getInstance().getZooKeeper(), protocol);
      isVersionRegistered = true;
    }
  }
}
