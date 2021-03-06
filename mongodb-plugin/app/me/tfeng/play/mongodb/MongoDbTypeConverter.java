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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.BSONTimestamp;
import org.bson.types.ObjectId;

import com.google.common.collect.ImmutableMap;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONCallback;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class MongoDbTypeConverter {

  public static interface Converter<S, T> {

    public T convertFromMongoDbType(S data);

    public S convertToMongoDbType(T object);
  }

  public static class BSONTimestampToLongConverter implements Converter<BSONTimestamp, Long> {

    @Override
    public Long convertFromMongoDbType(BSONTimestamp data) {
      int inc = data.getInc();
      if (inc < 0 || inc >= 1000) {
        throw new RuntimeException(
            "Overflow occurs while converting BSONTimestamp into long: " + data);
      }
      return (long) data.getTime() * 1000 + inc;
    }

    @Override
    public BSONTimestamp convertToMongoDbType(Long object) {
      return new BSONTimestamp((int) (object / 1000), (int) (object % 1000));
    }
  }

  public static class BSONTimestampToStringConverter implements Converter<BSONTimestamp, String> {

    @Override
    public String convertFromMongoDbType(BSONTimestamp timestamp) {
      return "(" + timestamp.getTime() + ", " + timestamp.getInc() + ")";
    }

    @Override
    public BSONTimestamp convertToMongoDbType(String data) {
      Matcher matcher = TIMESTAMP_PATTERN.matcher(data);
      if (!matcher.matches()) {
        throw new RuntimeException("Invalid BSONTimestamp " + data);
      }
      int time = Integer.parseInt(matcher.group(1));
      int inc = Integer.parseInt(matcher.group(2));
      return new BSONTimestamp(time, inc);
    }
  }

  public static class DateToLongConverter implements Converter<Date, Long> {

    @Override
    public Long convertFromMongoDbType(Date data) {
      return data.getTime();
    }

    @Override
    public Date convertToMongoDbType(Long object) {
      return new Date(object);
    }
  }

  public static class DateToStringConverter implements Converter<Date, String> {

    @Override
    public String convertFromMongoDbType(Date object) {
      return DATE_FORMAT.format(object);
    }

    @Override
    public Date convertToMongoDbType(String data) {
      try {
        return DATE_FORMAT.parse(data);
      } catch (ParseException e) {
        throw new RuntimeException("String does not confirm to date format: " + data);
      }
    }
  }

  public static class ObjectIdToStringConverter implements Converter<ObjectId, String> {

    @Override
    public String convertFromMongoDbType(ObjectId objectId) {
      return objectId.toString();
    }

    @Override
    public ObjectId convertToMongoDbType(String data) {
      return new ObjectId(data);
    }
  }

  public static final Map<Pair<Class<?>, Class<?>>, Converter<?, ?>> CONVERTER_MAP =
      ImmutableMap.<Pair<Class<?>, Class<?>>, Converter<?, ?>>builder()
          .put(ImmutablePair.of(BSONTimestamp.class, Long.class), new BSONTimestampToLongConverter())
          .put(ImmutablePair.of(BSONTimestamp.class, String.class), new BSONTimestampToStringConverter())
          .put(ImmutablePair.of(Date.class, Long.class), new DateToLongConverter())
          .put(ImmutablePair.of(Date.class, String.class), new DateToStringConverter())
          .put(ImmutablePair.of(ObjectId.class, String.class), new ObjectIdToStringConverter())
          .build();

  public static final DateFormat DATE_FORMAT = new SimpleDateFormat(JSONCallback._msDateFormat);

  public static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\(([0-9]+),\\s*([0-9]+)\\)");

  static {
    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public static <S, T> T convertFromMongoDbType(Class<T> dataClass, S object) {
    if (object == null) {
      return null;
    } else if (dataClass.isInstance(object)) {
      return dataClass.cast(object);
    } else {
      @SuppressWarnings("unchecked")
      Converter<S, T> converter =
          (Converter<S, T>) CONVERTER_MAP.get(ImmutablePair.of(object.getClass(), dataClass));
      if (converter != null) {
        return converter.convertFromMongoDbType(object);
      } else if (String.class.isAssignableFrom(dataClass) && object instanceof DBObject) {
        return dataClass.cast(JSON.serialize(object));
      } else {
        return null;
      }
    }
  }

  public static <S, T> S convertToMongoDbType(Class<S> mongoClass, T data) {
    if (data == null) {
      return null;
    } else if (mongoClass.isInstance(data)) {
      return mongoClass.cast(data);
    } else {
      @SuppressWarnings("unchecked")
      Converter<S, T> converter =
          (Converter<S, T>) CONVERTER_MAP.get(ImmutablePair.of(mongoClass, data.getClass()));
      if (converter != null) {
        return converter.convertToMongoDbType(data);
      } else if (DBObject.class.isAssignableFrom(mongoClass) && data instanceof String) {
        return mongoClass.cast(JSON.parse((String) data));
      } else {
        return null;
      }
    }
  }
}
