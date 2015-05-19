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

package me.tfeng.play.spring;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;

import me.tfeng.play.plugins.SpringPlugin;
import play.Application;
import play.GlobalSettings;
import play.libs.F.Promise;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class SpringGlobalSettings extends GlobalSettings {

  @Override
  public <A> A getControllerInstance(Class<A> clazz) {
    try {
      return SpringPlugin.getInstance().getApplicationContext().getBean(clazz);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Promise<Result> onError(RequestHeader request, Throwable t) {
    return Promise.pure(Results.badRequest());
  }

  @Override
  public void onStart(Application application) {
    AutowiredAnnotationBeanPostProcessor beanPostProcessor =
        new AutowiredAnnotationBeanPostProcessor();
    beanPostProcessor.setBeanFactory(getApplicationContext(application).getBeanFactory());
    beanPostProcessor.processInjection(this);
  }

  protected ConfigurableApplicationContext getApplicationContext(Application application) {
    return application.plugin(SpringPlugin.class).getApplicationContext();
  }
}
