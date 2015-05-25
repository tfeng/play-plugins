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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.IpcRequestor;
import org.apache.avro.specific.SpecificData;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import com.google.common.collect.Lists;

import me.tfeng.play.avro.AvroHelper;
import me.tfeng.play.avro.IpcRequestPreparerChain;
import me.tfeng.play.http.IpcRequestPreparer;
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

  private volatile boolean isVersionRegistered;

  private volatile int lastIndex = -1;

  private final IpcRequestPreparerChain postRequestPreparerChain =
      new IpcRequestPreparerChain();

  private final Protocol protocol;

  private volatile boolean refreshed;

  private final AvroD2ResponseProcessor responseProcessor = new AvroD2ResponseProcessor();

  private final List<URL> serverUrls = Lists.newArrayList();

  private boolean useGenericRecord;

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

  public void addPostRequestPreparer(IpcRequestPreparer postRequestPreparer) {
    postRequestPreparerChain.add(postRequestPreparer);
  }

  public synchronized URL getNextServerUrl() {
    if (serverUrls.isEmpty()) {
      throw new RuntimeException("No server is found for " + protocol.getName());
    } else {
      lastIndex = (lastIndex + 1) % serverUrls.size();
      return serverUrls.get(lastIndex);
    }
  }

  public Protocol getProtocol() {
    return protocol;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return setupRequest().invoke(proxy, method, args);
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

    synchronized(this) {
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

  public void removePostRequestPreparer(IpcRequestPreparer postRequestPreparer) {
    postRequestPreparerChain.remove(postRequestPreparer);
  }

  public Promise<Object> request(String message, Object[] request) throws Exception {
    return setupRequest().request(message, request);
  }

  public synchronized void setUseGenericRecord(boolean useGenericRecord) {
    this.useGenericRecord = useGenericRecord;
  }

  public synchronized boolean useGenericRecord() {
    return useGenericRecord;
  }

  private void scheduleRefresh() {
    AvroD2Plugin.getInstance().getScheduler().schedule(this::refresh,
        AvroD2Plugin.getInstance().getClientRefreshRetryDelay(), TimeUnit.MILLISECONDS);
  }

  private synchronized IpcRequestor setupRequest() throws IOException, KeeperException,
      InterruptedException {
    if (!refreshed) {
      refreshed = true;
      refresh();
    }

    if (!isVersionRegistered) {
      AvroD2Helper.createVersionNode(AvroD2Plugin.getInstance().getZooKeeper(), protocol);
      isVersionRegistered = true;
    }

    IpcRequestor requestor = new IpcRequestor(protocol, new AvroD2Transceiver(this), data);
    requestor.setUseGenericRecord(useGenericRecord);
    requestor.addRequestPreparer(postRequestPreparerChain);
    requestor.setResponseProcessor(responseProcessor);
    return requestor;
  }
}
