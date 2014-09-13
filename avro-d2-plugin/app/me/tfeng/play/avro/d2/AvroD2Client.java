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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import me.tfeng.play.avro.AvroHelper;
import me.tfeng.play.plugins.AvroD2Plugin;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.IpcRequestor;
import org.apache.avro.specific.SpecificData;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Client implements Watcher, InvocationHandler {

  private static final ALogger LOG = Logger.of(AvroD2Client.class);

  private final SpecificData data;
  private Supplier<Map<String, String>> headersSupplier;
  private final Class<?> interfaceClass;
  private int lastIndex = -1;
  private final Protocol protocol;
  private IpcRequestor requestor;

  private final List<URL> serverUrls = new ArrayList<>();

  public AvroD2Client(Class<?> interfaceClass) {
    this(interfaceClass, new SpecificData(interfaceClass.getClassLoader()));
  }

  public AvroD2Client(Class<?> interfaceClass, SpecificData data) {
    this.interfaceClass = interfaceClass;
    this.data = data;
    protocol = AvroHelper.getProtocol(interfaceClass);
  }

  public URL getNextServerUrl() {
    if (serverUrls.isEmpty()) {
      throw new RuntimeException("No server is found for " + AvroD2Helper.getUri(protocol));
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
    if (requestor == null) {
      refresh();
      try {
        requestor = new IpcRequestor(interfaceClass, new AvroD2Transceiver(this), data);
        requestor.setHeadersSupplier(headersSupplier);
      } catch (IOException e) {
        throw new RuntimeException("Unable to initialize Avro requestor for " + protocol.getName(),
            e);
      }
    }
    return requestor.invoke(proxy, method, args);
  }

  @Override
  public void process(WatchedEvent event) {
    refresh();
  }

  public void refresh() {
    String path = AvroD2Helper.getZkPath(protocol);
    ZooKeeper zk = AvroD2Plugin.getInstance().getZooKeeper();

    List<String> children;
    try {
      children = zk.getChildren(path, this);
    } catch (Exception e) {
      LOG.warn("Unable to list servers for " + protocol.getName() + "; retry later", e);
      scheduleRefresh();
      return;
    }

    serverUrls.clear();
    for (String child : children) {
      String childPath = path + "/" + child;
      try {
        byte[] data = zk.getData(childPath, false, null);
        String serverUrl = new String(data, Charset.forName("utf8"));
        serverUrls.add(new URL(serverUrl));
      } catch (Exception e) {
        LOG.warn("Unable to get server URL from node " + childPath, e);
      }
    }

    if (serverUrls.isEmpty()) {
      LOG.warn("Unable to get any server URL for protocol " + protocol.getName() + "; retry later");
      scheduleRefresh();
    }
  }

  public void setHeadersSupplier(Supplier<Map<String, String>> headersSupplier) {
    this.headersSupplier = headersSupplier;
    if (requestor != null) {
      requestor.setHeadersSupplier(headersSupplier);
    }
  }

  private void scheduleRefresh() {
    AvroD2Plugin.getInstance().getScheduler().schedule(() -> refresh(),
        AvroD2Plugin.getInstance().getClientRefreshRetryDelay(), TimeUnit.MILLISECONDS);
  }
}
