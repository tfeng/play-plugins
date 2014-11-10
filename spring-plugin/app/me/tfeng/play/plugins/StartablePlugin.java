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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;

import me.tfeng.play.spring.ExtendedStartable;
import me.tfeng.play.spring.Startable;
import me.tfeng.play.utils.DependencyUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.Play;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class StartablePlugin extends AbstractPlugin {

  private static final ALogger LOG = Logger.of(StartablePlugin.class);

  public static StartablePlugin getInstance() {
    return Play.application().plugin(StartablePlugin.class);
  }

  private Set<Startable> startables = new HashSet<>();

  public StartablePlugin(Application application) {
    super(application);
  }

  public void addStartable(Startable startable) {
    startables.add(startable);
  }

  public void addStartables(Collection<? extends Startable> startables) {
    this.startables.addAll(startables);
  }

  public List<Startable> getStartables() {
    BeanDefinitionRegistry registry =
        (BeanDefinitionRegistry) getApplicationContext().getAutowireCapableBeanFactory();
    Comparator<Entry<String, Startable>> beanDependencyComparator =
        (bean1, bean2) -> {
          String beanName1 = bean1.getKey();
          String beanName2 = bean2.getKey();
          BeanDefinition beanDefinition1 = registry.getBeanDefinition(beanName1);
          BeanDefinition beanDefinition2 = registry.getBeanDefinition(beanName2);
          if (beanDefinition1 == null || beanDefinition2 == null) {
            return 0;
          } else if (ArrayUtils.contains(beanDefinition1.getDependsOn(), beanName2)) {
            return 1;
          } else if (ArrayUtils.contains(beanDefinition2.getDependsOn(), beanName1)) {
            return -1;
          } else {
            return 0;
          }
        };
    Set<Entry<String, Startable>> entries =
        getApplicationContext().getBeansOfType(Startable.class).entrySet();
    List<Entry<String, Startable>> sortedEntries =
        DependencyUtils.dependencySort(entries, beanDependencyComparator);
    List<Startable> result = new ArrayList<>(startables.size() + sortedEntries.size());
    result.addAll(startables);
    for (Entry<String, Startable> entry : sortedEntries) {
      Startable startable = entry.getValue();
      if (!startables.contains(startable)) {
        result.add(startable);
      }
    }
    return result;
  }

  @Override
  public void onStart() {
    super.onStart();

    List<Startable> startables = getStartables();

    for (Startable startable: startables) {
      if (startable instanceof ExtendedStartable) {
        try {
          ((ExtendedStartable) startable).beforeStart();
        } catch (Throwable t) {
          onStartFailure(startable, t);
        }
      }
    }

    for (Startable startable: startables) {
      try {
        startable.onStart();
      } catch (Throwable t) {
        onStartFailure(startable, t);
      }
    }

    for (Startable startable: startables) {
      if (startable instanceof ExtendedStartable) {
        try {
          ((ExtendedStartable) startable).afterStart();
        } catch (Throwable t) {
          onStartFailure(startable, t);
        }
      }
    }
  }

  public void onStartFailure(Startable startable, Throwable t) {
    throw new RuntimeException("Unable to start " + startable, t);
  }

  @Override
  public void onStop() {
    List<Startable> startables = getStartables();

    for (ListIterator<Startable> iterator = startables.listIterator(startables.size());
        iterator.hasPrevious();) {
      Startable startable = iterator.previous();
      if (startable instanceof ExtendedStartable) {
        try {
          ((ExtendedStartable) startable).beforeStop();
        } catch (Throwable t) {
          onStartFailure(startable, t);
        }
      }
    }

    for (ListIterator<Startable> iterator = startables.listIterator(startables.size());
        iterator.hasPrevious();) {
      Startable startable = iterator.previous();
      try {
        startable.onStop();
      } catch (Throwable t) {
        onStopFailure(startable, t);
      }
    }

    for (ListIterator<Startable> iterator = startables.listIterator(startables.size());
        iterator.hasPrevious();) {
      Startable startable = iterator.previous();
      if (startable instanceof ExtendedStartable) {
        try {
          ((ExtendedStartable) startable).afterStop();
        } catch (Throwable t) {
          onStartFailure(startable, t);
        }
      }
    }

    super.onStop();
  }

  public void onStopFailure(Startable startable, Throwable t) {
    LOG.error("Unable to stop " + startable, t);
  }
}
