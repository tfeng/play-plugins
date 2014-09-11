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

package me.tfeng.play.factories;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

import play.libs.Akka;
import play.libs.F.Promise;
import scala.concurrent.ExecutionContext;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroLocalClientFactory implements FactoryBean<Object>, InvocationHandler {

  private Object bean;
  private String executionContextId;
  private Class<?> interfaceClass;

  @Override
  public Object getObject() throws Exception {
    return Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass },
        this);
  }

  @Override
  public Class<?> getObjectType() {
    return interfaceClass;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Method beanMethod = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
    if (executionContextId == null) {
      return Promise.promise(() -> beanMethod.invoke(bean, args));
    } else {
      ExecutionContext executionContext = Akka.system().dispatchers().lookup(executionContextId);
      return Promise.promise(() -> beanMethod.invoke(bean, args), executionContext);
    }
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  @Required
  public void setBean(Object bean) {
    this.bean = bean;
  }

  public void setExecutionContextId(String executionContextId) {
    this.executionContextId = executionContextId;
  }

  @Required
  public void setInterfaceClass(Class<?> interfaceClass) {
    this.interfaceClass = interfaceClass;
  }
}
