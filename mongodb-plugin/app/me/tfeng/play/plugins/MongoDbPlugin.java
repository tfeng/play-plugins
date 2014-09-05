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

import java.util.List;
import java.util.stream.Collectors;

import me.tfeng.play.mongodb.OplogListener;
import me.tfeng.play.spring.Startable;
import play.Application;
import play.Play;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class MongoDbPlugin extends StartablePlugin {

  public static MongoDbPlugin getInstance() {
    return Play.application().plugin(MongoDbPlugin.class);
  }

  public MongoDbPlugin(Application application) {
    super(application);
  }

  @Override
  protected List<Startable> getStartables() {
    return getApplicationContext().getBeansOfType(OplogListener.class).entrySet().stream()
        .sorted((entry1, entry2) -> entry1.getKey().compareTo(entry2.getKey()))
        .map(entry -> entry.getValue())
        .collect(Collectors.toList());
  }
}
