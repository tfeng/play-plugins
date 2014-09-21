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

package me.tfeng.play.spring.test;

import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class MockingBeanPostProcessor implements BeanPostProcessor {

  private Map<String, Class<?>> beans;
  private List<Class<?>> classes;

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (beans != null) {
      Class<?> beanClass = beans.get(beanName);
      if (beanClass != null) {
        return Mockito.mock(beanClass);
      }
    }

    if (classes != null) {
      for (Class<?> beanClass : classes) {
        if (beanClass.isInstance(bean)) {
          return Mockito.mock(beanClass);
        }
      }
    }

    return bean;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    return bean;
  }

  public void setBeans(Map<String, Class<?>> beans) {
    this.beans = beans;
  }

  public void setClasses(List<Class<?>> classes) {
    this.classes = classes;
  }
}
