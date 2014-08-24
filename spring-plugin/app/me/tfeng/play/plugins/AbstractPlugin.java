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

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;

import play.Application;
import play.Configuration;
import play.Plugin;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AbstractPlugin<T extends Plugin> extends Plugin {

  private Application application;

  public AbstractPlugin(Application application) {
    this.application = application;
  }

  public ConfigurableApplicationContext getApplicationContext() {
    return application.plugin(SpringPlugin.class).getApplicationContext();
  }

  @Override
  public void onStart() {
    AutowiredAnnotationBeanPostProcessor beanPostProcessor =
        new AutowiredAnnotationBeanPostProcessor();
    beanPostProcessor.setBeanFactory(getApplicationContext().getBeanFactory());
    beanPostProcessor.processInjection(this);
  }

  protected Application getApplication() {
    return application;
  }

  protected Configuration getConfiguration() {
    return application.configuration();
  }
}
