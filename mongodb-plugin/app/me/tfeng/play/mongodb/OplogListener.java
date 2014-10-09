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

package me.tfeng.play.mongodb;

import java.util.concurrent.atomic.AtomicBoolean;

import me.tfeng.play.spring.Startable;

import org.bson.types.BSONTimestamp;

import play.Logger;
import play.Logger.ALogger;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class OplogListener implements Startable {

  private class OplogListenerThread implements Runnable {

    @Override
    public void run() {
      DBObject object;
      do {
        try {
          object = cursor.next();
        } catch (Exception e) {
          if (stopping.get()) {
            break;
          } else if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
          } else {
            throw new RuntimeException(
                "Unexpected exception occurred while trying to read the next oplog item", e);
          }
        }

        if (LOG.isDebugEnabled()) {
          LOG.info("Received oplog item " + object);
        }

        OplogItem oplogItem = RecordConverter.toRecord(OplogItem.class, object);
        handler.handle(oplogItem);
      } while (!stopping.get());
    }
  }

  public static final String COLLECTION_NAME = "oplog.rs";

  public static final String DB_NAME = "local";

  private static final ALogger LOG = Logger.of(OplogListener.class);

  private static final AtomicBoolean stopping = new AtomicBoolean(false);

  private DBCollection collection;

  private DBCursor cursor;

  private OplogItemHandler handler;

  private MongoClient mongoClient;

  private String namespace;

  private BSONTimestamp startTimestamp;

  private Thread thread;

  @Override
  public void onStart() throws Throwable {
    if (mongoClient == null || handler == null) {
      throw new Exception("mongoClient and handler must both be provided");
    }

    LOG.info("Connecting to " + DB_NAME + "." + COLLECTION_NAME + " in MongoDB");
    collection = mongoClient.getDB(DB_NAME).getCollection(COLLECTION_NAME);
    cursor = collection.find(getQuery()).sort(getSort()).setOptions(getOptions());

    stopping.set(false);

    thread = new Thread(new OplogListenerThread());
    thread.start();
    LOG.info("Handler thread started");
  }

  @Override
  public void onStop() throws Throwable {
    stopping.set(true);
    cursor.close();
    LOG.info("Waiting for handler thread to stop");
    thread.join();
  }

  public void setHandler(OplogItemHandler handler) {
    this.handler = handler;
  }

  public void setMongoClient(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public void setStartTimestamp(BSONTimestamp startTimestamp) {
    this.startTimestamp = startTimestamp;
  }

  protected int getOptions() {
    return Bytes.QUERYOPTION_TAILABLE;
  }

  protected DBObject getQuery() {
    DBObject query = new BasicDBObject();
    if (startTimestamp != null) {
      query.put("ts", new BasicDBObject("$gt", startTimestamp));
    }
    if (namespace != null) {
      query.put("ns", namespace);
    }
    return query;
  }

  protected DBObject getSort() {
    return new BasicDBObject("$natural", 1);
  }
}
