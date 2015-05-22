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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.avro.ipc.AsyncHttpTransceiver;
import org.apache.avro.ipc.IpcRequestor;
import org.apache.avro.specific.SpecificData;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;

import me.tfeng.play.avro.AsyncTransceiver;
import me.tfeng.play.avro.HandshakingProtocolVersionResolver;
import me.tfeng.play.avro.ProtocolVersionResolver;
import me.tfeng.play.http.PostRequestPreparer;
import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.libs.Akka;
import play.libs.HttpExecution;
import scala.concurrent.ExecutionContext;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroPlugin extends AbstractPlugin {

  private static final ALogger LOG = Logger.of(AvroPlugin.class);

  public static <T> T client(Class<T> interfaceClass, AsyncTransceiver transceiver,
      PostRequestPreparer... postRequestPreparers) {
    return client(interfaceClass, transceiver, new SpecificData(interfaceClass.getClassLoader()),
        postRequestPreparers);
  }

  @SuppressWarnings("unchecked")
  public static <T> T client(Class<T> interfaceClass, AsyncTransceiver transceiver,
      SpecificData data, PostRequestPreparer... postRequestPreparers) {
    try {
      IpcRequestor requestor = new IpcRequestor(interfaceClass, transceiver, data);
      Arrays.stream(postRequestPreparers).forEach(requestor::addPostRequestPreparer);
      return (T) Proxy.newProxyInstance(data.getClassLoader(), new Class[] { interfaceClass },
          requestor);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create async client", e);
    }
  }

  public static <T> T client(Class<T> interfaceClass, URL url,
      PostRequestPreparer... postRequestPreparers) {
    return client(interfaceClass, new AsyncHttpTransceiver(url), postRequestPreparers);
  }

  public static <T> T client(Class<T> interfaceClass, URL url, SpecificData data,
      PostRequestPreparer... postRequestPreparers) {
    return client(interfaceClass, new AsyncHttpTransceiver(url), data, postRequestPreparers);
  }

  public static AvroPlugin getInstance() {
    return Play.application().plugin(AvroPlugin.class);
  }

  private ExecutionContext executionContext;

  @Value("${avro-plugin.execution-context:play.akka.actor.default-dispatcher}")
  private String executionContextId;

  private Map<Class<?>, Object> protocolImplementations;

  private ProtocolVersionResolver protocolVersionResolver =
      new HandshakingProtocolVersionResolver();

  public AvroPlugin(Application application) {
    super(application);
  }

  public ExecutionContext getExecutionContext() {
    return HttpExecution.fromThread(executionContext);
  }

  public Map<Class<?>, Object> getProtocolImplementations() {
    return protocolImplementations;
  }

  public ProtocolVersionResolver getProtocolVersionResolver() {
    return protocolVersionResolver;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart() {
    super.onStart();

    try {
      executionContext = Akka.system().dispatchers().lookup(executionContextId);
    } catch (Exception e) {
      LOG.warn("Unable to obtain execution context " + executionContextId + "; using default", e);
      executionContext = Akka.system().dispatchers().defaultGlobalDispatcher();
    }

    try {
      protocolImplementations =
          getApplicationContext().getBean("avro-plugin.protocol-implementations", Map.class);
    } catch (NoSuchBeanDefinitionException e) {
      protocolImplementations = Collections.emptyMap();
    }
  }

  public void setProtocolVersionResolver(ProtocolVersionResolver resolver) {
    protocolVersionResolver = resolver;
  }
}
