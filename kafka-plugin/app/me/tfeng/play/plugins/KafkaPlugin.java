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

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import play.Application;
import play.Play;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class KafkaPlugin extends AbstractPlugin {

  public static KafkaPlugin getInstance() {
    return Play.application().plugin(KafkaPlugin.class);
  }

  @Autowired(required = false)
  @Qualifier("kafka-plugin.consumer-properties")
  private Properties consumerProperties;

  @Autowired(required = false)
  @Qualifier("kafka-plugin.producer-properties")
  private Properties producerProperties;

  public KafkaPlugin(Application application) {
    super(application);
  }

  public ConsumerConnector createConsumerConnector() {
    return Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
  }

  public <K, V> Producer<K, V> createProducer() {
    return new Producer<K, V>(new ProducerConfig(producerProperties));
  }
}
