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

package me.tfeng.play.plugins;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.avro.Protocol;
import org.apache.avro.specific.SpecificData;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;

import me.tfeng.play.avro.AvroHelper;
import me.tfeng.play.avro.d2.AvroD2Client;
import me.tfeng.play.avro.d2.AvroD2Helper;
import me.tfeng.play.avro.d2.AvroD2Server;
import me.tfeng.play.http.PostRequestPreparer;
import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Plugin extends AbstractPlugin implements Watcher {

  private static final ALogger LOG = Logger.of(AvroD2Plugin.class);

  public static <T> T client(Class<T> interfaceClass) {
    return client(interfaceClass, new SpecificData(interfaceClass.getClassLoader()));
  }

  public static <T> T client(Class<T> interfaceClass, PostRequestPreparer postRequestPreparer) {
    return client(interfaceClass, new SpecificData(interfaceClass.getClassLoader()),
        postRequestPreparer);
  }

  public static <T> T client(Class<T> interfaceClass, SpecificData data) {
    return client(interfaceClass, data, null);
  }

  public static <T> T client(Class<T> interfaceClass, SpecificData data,
      PostRequestPreparer postRequestPreparer) {
    AvroD2Client client = new AvroD2Client(interfaceClass, data);
    client.setPostRequestPreparer(postRequestPreparer);
    return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(),
        new Class<?>[] { interfaceClass }, client));
  }

  public static AvroD2Plugin getInstance() {
    return Play.application().plugin(AvroD2Plugin.class);
  }

  @Value("${avro-d2-plugin.client-refresh-retry-delay-ms:1000}")
  private long clientRefreshRetryDelay;

  private boolean expired;

  private Map<Class<?>, String> protocolPaths;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @Value("${avro-d2-plugin.server-host}")
  private String serverHost;

  @Value("${avro-d2-plugin.server-port}")
  private int serverPort;

  @Value("${avro-d2-plugin.server-register-retry-delay-ms:1000}")
  private long serverRegisterRetryDelay;

  private List<AvroD2Server> servers;

  private ZooKeeper zk;

  @Value("${avro-d2-plugin.zk-connect-string}")
  private String zkConnectString;

  @Value("${avro-d2-plugin.zk-session-timeout:10000}")
  private int zkSessionTimeout;

  public AvroD2Plugin(Application application) {
    super(application);
  }

  public void connect() {
    try {
      zk = new ZooKeeper(zkConnectString, zkSessionTimeout, this);
    } catch (IOException e) {
      getScheduler().schedule(() -> connect(), getClientRefreshRetryDelay(), TimeUnit.MILLISECONDS);
      LOG.warn("Unable to connect to ZooKeeper; retry later", e);
    }
  }

  public long getClientRefreshRetryDelay() {
    return clientRefreshRetryDelay;
  }

  public ScheduledExecutorService getScheduler() {
    return scheduler;
  }

  public long getServerRegisterRetryDelay() {
    return serverRegisterRetryDelay;
  }

  public ZooKeeper getZooKeeper() {
    return zk;
  }

  public boolean isRegistered(Class<?> interfaceClass) {
    if (zk == null) {
      return false;
    }
    String path = AvroD2Helper.getServersZkPath(AvroHelper.getProtocol(interfaceClass));
    try {
      return !zk.getChildren(path, this).isEmpty();
    } catch (KeeperException | InterruptedException e) {
      return false;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart() {
    super.onStart();

    try {
      protocolPaths = getApplicationContext().getBean("avro-d2-plugin.protocol-paths", Map.class);
    } catch(NoSuchBeanDefinitionException e) {
      protocolPaths = Collections.emptyMap();
    }

    connect();
    startServers();
  }

  @Override
  public void onStop() {
    stopServers();
  }

  @Override
  public void process(WatchedEvent event) {
    LOG.info(event.toString());
    switch (event.getState()) {
    case SyncConnected:
      if (expired) {
        expired = false;
        servers.forEach(server -> server.register());
      }
      break;
    case Expired:
      expired = true;
      try {
        zk.close();
      } catch (InterruptedException e) {
        // Ignore.
      }
      connect();
    default:
    }
  }

  private void startServers() {
    servers = new ArrayList<>(protocolPaths.size());
    for (Entry<Class<?>, String> entry : protocolPaths.entrySet()) {
      Protocol protocol = AvroHelper.getProtocol(entry.getKey());
      String path = entry.getValue();
      if (!path.startsWith("/")) {
        path = "/" + path;
      }
      URL url;
      try {
        url = new URL("http", serverHost, serverPort, path);
      } catch (Exception e) {
        throw new RuntimeException("Unable to initialize server", e);
      }
      AvroD2Server server = new AvroD2Server(protocol, url);
      servers.add(server);
    }
    StartablePlugin.getInstance().addStartables(servers);
  }

  private void stopServers() {
    servers.stream().forEach(server -> {
      try {
        server.close();
      } catch (Exception e) {
        LOG.error("Unable to close server for " + server.getProtocol().getName() + " at "
            + server.getUrl() + "; ignoring");
      }
    });
    servers.clear();
  }
}
