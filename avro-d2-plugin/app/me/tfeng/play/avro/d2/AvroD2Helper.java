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

package me.tfeng.play.avro.d2;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.avro.Protocol;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Helper {

  public static final String SCHEME = "avsd";

  public static final Charset UTF8 = Charset.forName("utf8");

  public static String createProtocolNode(ZooKeeper zk, Protocol protocol, URL serverUrl)
      throws KeeperException, InterruptedException {
    ensurePath(zk, getZkPath(protocol));
    return zk.create(AvroD2Helper.getZkPath(protocol) + "/", serverUrl.toString().getBytes(),
        Ids.READ_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
  }

  public static void ensurePath(ZooKeeper zk, String path) throws KeeperException,
      InterruptedException {
    int index = path.lastIndexOf("/");
    if (index > 0) {
      ensurePath(zk, path.substring(0, index));
    }
    String part = path.substring(index + 1);
    if (!part.isEmpty()) {
      try {
        zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      } catch (NodeExistsException e) {
        // Ignore.
      }
    }
  }

  public static URI getUri(Protocol protocol) {
    try {
      return new URI(SCHEME + ":/" + getZkPath(protocol));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getZkPath(Protocol protocol) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      digest.update(protocol.getMD5());
      String md5 = new BigInteger(1, digest.digest()).toString(16);
      return "/" + protocol.getName() + "/" + md5;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
