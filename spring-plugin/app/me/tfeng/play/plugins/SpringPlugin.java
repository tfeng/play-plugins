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

import java.util.Collections;
import java.util.List;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.AbstractEnvironment;

import com.google.common.collect.Lists;

import me.tfeng.play.spring.ApplicationContextHolder;
import me.tfeng.play.spring.WithSpringConfig;
import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.api.Plugin;
import play.libs.Scala;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class SpringPlugin extends AbstractPlugin {

  private static final ALogger LOG = Logger.of(SpringPlugin.class);

  public static SpringPlugin getInstance() {
    return Play.application().plugin(SpringPlugin.class);
  }

  private ConfigurableApplicationContext applicationContext;

  public SpringPlugin(Application application) {
    super(application);
  }

  @Override
  public ConfigurableApplicationContext getApplicationContext() {
    return applicationContext;
  }

  @Override
  public void onStart() {
    applicationContext = ApplicationContextHolder.get();

    if (applicationContext == null) {
      List<String> configLocations =
          Lists.newArrayList(getConfiguration().getStringList("spring-plugin.spring-config-locations",
              Collections.singletonList("classpath*:spring/**/*.xml")));

      List<Plugin> plugins = Scala.asJava(getApplication().getWrappedApplication().plugins());
      for (play.api.Plugin plugin : plugins) {
        WithSpringConfig annotation = plugin.getClass().getAnnotation(WithSpringConfig.class);
        if (annotation != null) {
          Collections.addAll(configLocations, annotation.value());
        }
      }
      LOG.info("Starting spring application context with config locations " + configLocations);

      ClassPathXmlApplicationContext classPathApplicationContext =
          new ClassPathXmlApplicationContext();
      List<String> activeProfiles =
          getConfiguration().getStringList(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
      List<String> defaultProfiles =
          getConfiguration().getStringList(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
      if (activeProfiles != null) {
        classPathApplicationContext.getEnvironment().setActiveProfiles(
            activeProfiles.toArray(new String[activeProfiles.size()]));
      }
      if (defaultProfiles != null) {
        classPathApplicationContext.getEnvironment().setDefaultProfiles(
            defaultProfiles.toArray(new String[defaultProfiles.size()]));
      }
      classPathApplicationContext.setConfigLocations(
          configLocations.toArray(new String[configLocations.size()]));
      classPathApplicationContext.refresh();
      applicationContext = classPathApplicationContext;

    } else {
      LOG.info("Using spring application context in ApplicationContextHolder");
    }

    super.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();

    LOG.info("Stopping spring application context");
    applicationContext.close();
  }
}
