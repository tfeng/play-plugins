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

import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.tfeng.play.avro.AvroHelper;
import me.tfeng.play.avro.d2.AvroD2Client;
import me.tfeng.play.avro.d2.AvroD2Helper;
import me.tfeng.play.avro.d2.AvroD2Server;

import org.apache.avro.Protocol;
import org.apache.avro.specific.SpecificData;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;

import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Plugin extends AbstractPlugin implements Watcher {

  private static final ALogger LOG = Logger.of(AvroD2Plugin.class);

  public static AvroD2Plugin getInstance() {
    return Play.application().plugin(AvroD2Plugin.class);
  }

  private final Map<URI, AvroD2Client> clients = new HashMap<>();

  private Map<Class<?>, String> protocolPaths;

  @Value("${avro-d2-plugin.server-host}")
  private String serverHost;

  @Value("${avro-d2-plugin.server-port}")
  private int serverPort;

  private List<AvroD2Server> servers;

  private ZooKeeper zk;

  @Value("${avro-d2-plugin.zk-connect-string}")
  private String zkConnectString;

  @Value("${avro-d2-plugin.zk-session-timeout:10000}")
  private int zkSessionTimeout;

  public AvroD2Plugin(Application application) {
    super(application);
  }

  public <T> T client(Class<T> interfaceClass) {
    return client(interfaceClass, new SpecificData(interfaceClass.getClassLoader()));
  }

  public <T> T client(Class<T> interfaceClass, SpecificData data) {
    URI uri = AvroD2Helper.getUri(AvroHelper.getProtocol(interfaceClass));
    AvroD2Client client = clients.get(uri);
    if (client == null) {
      client = new AvroD2Client(zk, interfaceClass, data);
      clients.put(uri, client);
    }
    return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(),
        new Class<?>[] { interfaceClass }, client));
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart() {
    super.onStart();

    protocolPaths = getApplicationContext().getBean("avro-d2-plugin.protocol-paths", Map.class);

    try {
      zk = new ZooKeeper(zkConnectString, zkSessionTimeout, this);
      servers = new ArrayList<>(protocolPaths.size());
      for (Entry<Class<?>, String> entry : protocolPaths.entrySet()) {
        Protocol protocol = AvroHelper.getProtocol(entry.getKey());
        String path = entry.getValue();
        if (!path.startsWith("/")) {
          path = "/" + path;
        }
        URL url = new URL("http", serverHost, serverPort, path);
        AvroD2Server server = new AvroD2Server(zk, protocol, url);
        servers.add(server);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to initialize server", e);
    }
  }

  @Override
  public void onStop() {
    stopServers();
  }

  @Override
  public void process(WatchedEvent event) {
    LOG.info(event.toString());
  }

  public void refreshClients() {
    clients.clear();
  }

  public void stopServers() {
    servers.stream().forEach(server -> server.close());
    servers.clear();
  }
}
