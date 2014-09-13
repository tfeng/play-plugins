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
import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import me.tfeng.play.avro.AvroHelper;
import me.tfeng.play.mongodb.MongoDbTypeConverter;
import me.tfeng.play.mongodb.RecordConverter;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.bson.types.BSONTimestamp;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.junit.Test;

import play.libs.Json;
import test.Ids;
import test.Names;
import test.Types1;
import test.Types2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class TestDBObjectDecoder {

  @Test
  public void testArrays() throws Exception {
    Schema schema = getSchema("schemata/arrays.avsc");

    GenericRecordBuilder builder = new GenericRecordBuilder(schema);
    builder.set("arrays", ImmutableList.of(ImmutableList.of(ImmutableList.of(1, 2, 3),
        ImmutableList.of()), ImmutableList.of(ImmutableList.of(4), ImmutableList.of()),
        ImmutableList.of(ImmutableList.of())));
    Record record1 = builder.build();

    String json = "{\"arrays\": [[[1, 2, 3], []], [[4], []], [[]]]}";
    DBObject object = (DBObject) JSON.parse(json);
    Record record2 = RecordConverter.toRecord(schema, object, getClass().getClassLoader());

    assertThat(record2).isEqualTo(record1);
    assertThat(AvroHelper.toJson(schema, record2)).isEqualTo(AvroHelper.toJson(schema, record1));
  }

  @Test
  public void testEmpty() throws Exception {
    Schema schema = getSchema("schemata/empty.avsc");

    GenericRecordBuilder builder = new GenericRecordBuilder(schema);
    Record record1 = builder.build();

    String json = "{}";
    DBObject object = (DBObject) JSON.parse(json);
    Record record2 = RecordConverter.toRecord(schema, object, getClass().getClassLoader());

    assertThat(record2).isEqualTo(record1);
    assertThat(AvroHelper.toJson(schema, record2)).isEqualTo(AvroHelper.toJson(schema, record1));
  }

  @Test
  public void testEnums() throws Exception {
    Schema schema = getSchema("schemata/enums.avsc");

    String avroJson = "{\"enum1\": \"X\", \"enum2\": {\"test.Enum2\": \"A\"}, \"enum3\": {\"null\": null}, \"enum4\": [{\"test.Enum4\": \"SAT\"}, {\"test.Enum4\": \"SUN\"}]}}";
    Decoder decoder = DecoderFactory.get().jsonDecoder(schema, avroJson);
    GenericDatumReader<Record> reader = new GenericDatumReader<Record>(schema);
    Record record1 = reader.read(null, decoder);

    String mongoJson = "{\"enum1\": \"X\", \"enum2\": \"A\", \"enum3\": null, \"enum4\": [\"SAT\", \"SUN\"]}}";
    DBObject object = (DBObject) JSON.parse(mongoJson);
    Record record2 = RecordConverter.toRecord(schema, object, getClass().getClassLoader());

    assertThat(record2).isEqualTo(record1);
    assertThat(AvroHelper.toJson(schema, record2)).isEqualTo(AvroHelper.toJson(schema, record1));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testIds() throws Exception {
    Schema schema = getSchema("schemata/ids.avsc");

    String avroJson = "{\"id\": \"5401bf578de2a77380c5489a\", \"nested\": {\"id\": \"6401bf578de2a77380c5489a\"}}";
    Decoder decoder = DecoderFactory.get().jsonDecoder(schema, avroJson);
    SpecificDatumReader<Ids> reader = new SpecificDatumReader<Ids>(schema);
    Ids ids1 = reader.read(null, decoder);

    DBObject object = new BasicDBObject();
    object.put("_id", new ObjectId("5401bf578de2a77380c5489a"));
    object.put("nested", new BasicDBObject("_id", new ObjectId("6401bf578de2a77380c5489a")));
    Ids ids2 = RecordConverter.toRecord(Ids.class, object);

    assertThat(ids1.getId().toString()).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(ids2.getId().toString()).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(ids1.getNested().getId().toString()).isEqualTo("6401bf578de2a77380c5489a");
    assertThat(ids2.getNested().getId().toString()).isEqualTo("6401bf578de2a77380c5489a");

    DBObject object1 = RecordConverter.toDbObject(ids1);
    DBObject object2 = RecordConverter.toDbObject(ids2);

    assertThat(object1.get("_id")).isEqualTo(new ObjectId("5401bf578de2a77380c5489a"));
    assertThat(object2.get("_id")).isEqualTo(new ObjectId("5401bf578de2a77380c5489a"));
    assertThat(((Map<String, Object>) object1.get("nested")).get("_id"))
        .isEqualTo(new ObjectId("6401bf578de2a77380c5489a"));
    assertThat(((Map<String, Object>) object2.get("nested")).get("_id"))
        .isEqualTo(new ObjectId("6401bf578de2a77380c5489a"));
  }

  @Test
  public void testMaps() throws Exception {
    Schema schema = getSchema("schemata/maps.avsc");

    GenericRecordBuilder builder = new GenericRecordBuilder(schema);
    builder.set("maps", ImmutableMap.of("key1", ImmutableMap.of("value1", 1, "value2", 2), "key2",
        ImmutableMap.of(), "key3", ImmutableMap.of("value3", 3)));
    Record record1 = builder.build();

    String json = "{\"maps\": {\"key1\": {\"value1\": 1, \"value2\": 2}, \"key2\": {}, \"key3\": {\"value3\": 3}}}";
    DBObject object = (DBObject) JSON.parse(json);
    Record record2 = RecordConverter.toRecord(schema, object, getClass().getClassLoader());

    // Convert into JsonNode before comparison, so the maps equal even if keys are reordered.
    assertThat((Object) Json.parse(AvroHelper.toJson(schema, record2)))
        .isEqualTo(Json.parse(AvroHelper.toJson(schema, record1)));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testNames() throws Exception {
    Schema schema = getSchema("schemata/names.avsc");

    String avroJson = "{\"id\": \"5401bf578de2a77380c5489a\", \"nested\": {\"id\": \"5401bf578de2a77380c5489b\"}}";
    Decoder decoder = DecoderFactory.get().jsonDecoder(schema, avroJson);
    SpecificDatumReader<Names> reader = new SpecificDatumReader<Names>(schema);
    Names names1 = reader.read(null, decoder);

    String mongoJson = "{\"_id\": \"5401bf578de2a77380c5489a\", \"nested\": {\"_id\": \"5401bf578de2a77380c5489b\"}}";
    DBObject object = (DBObject) JSON.parse(mongoJson);
    Names names2 = RecordConverter.toRecord(Names.class, object);

    assertThat(names1.getId().toString()).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(names2.getId().toString()).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(names1.getNested().getId().toString()).isEqualTo("5401bf578de2a77380c5489b");
    assertThat(names2.getNested().getId().toString()).isEqualTo("5401bf578de2a77380c5489b");

    DBObject object1 = RecordConverter.toDbObject(names1);
    DBObject object2 = RecordConverter.toDbObject(names2);

    assertThat(object1.get("_id")).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(object2.get("_id")).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(((Map<String, Object>) object1.get("nested")).get("_id"))
        .isEqualTo("5401bf578de2a77380c5489b");
    assertThat(((Map<String, Object>) object2.get("nested")).get("_id"))
        .isEqualTo("5401bf578de2a77380c5489b");
  }

  @Test
  public void testPrimitives() throws Exception {
    Schema schema = getSchema("schemata/primitives.avsc");

    GenericRecordBuilder builder = new GenericRecordBuilder(schema);
    builder.set("i", 1);
    builder.set("l", 2l);
    builder.set("s", "This is a string");
    builder.set("b", true);
    builder.set("f", 3.1f);
    builder.set("d", 4.2);
    builder.set("n", null);
    builder.set("by", ByteBuffer.wrap("This is a string in bytes".getBytes()));
    Record record1 = builder.build();

    DBObject object = new BasicDBObject();
    object.put("i", 1);
    object.put("l", 2l);
    object.put("s", "This is a string");
    object.put("b", true);
    object.put("f", 3.1f);
    object.put("d", 4.2);
    object.put("n", null);
    object.put("by", new Binary("This is a string in bytes".getBytes()));
    Record record2 = RecordConverter.toRecord(schema, object, getClass().getClassLoader());

    assertThat(record2).isEqualTo(record1);
    assertThat(AvroHelper.toJson(schema, record2)).isEqualTo(AvroHelper.toJson(schema, record1));
  }

  @Test
  public void testRecords() throws Exception {
    Schema schema = getSchema("schemata/records.avsc");

    GenericRecordBuilder builder = new GenericRecordBuilder(schema);
    builder.set("record1",
        new GenericRecordBuilder(schema.getField("record1").schema()).set("i", 1).build());
    Record _record21 =
        new GenericRecordBuilder(schema.getField("record2").schema().getField("record21").schema())
            .set("s", "This is a string").build();
    Record _record2 =
        new GenericRecordBuilder(schema.getField("record2").schema()).set("l", 2l)
            .set("record21", _record21).build();
    builder.set("record2", _record2);
    Record _record311 = new GenericRecordBuilder(schema.getField("record3").schema()
        .getField("record31").schema().getField("record311").schema()).set("d", 4.2).build();
    Record _record31 = new GenericRecordBuilder(schema.getField("record3").schema()
        .getField("record31").schema()).set("f", 3.1f).set("record311", _record311).build();
    Record _record32 = new GenericRecordBuilder(schema.getField("record3").schema()
        .getField("record32").schema()).set("b", true).build();
    Record _record3 = new GenericRecordBuilder(schema.getField("record3").schema())
        .set("record31", _record31).set("record32", _record32).build();
    builder.set("record3", _record3);
    Record record1 = builder.build();

    String json = "{\"record1\": {\"i\": 1}, \"record2\": {\"l\": 2, \"record21\": {\"s\": \"This is a string\"}}, \"record3\": {\"record31\": {\"f\": 3.1, \"record311\": {\"d\": 4.2}}, \"record32\": {\"b\": true}}}";
    DBObject object = (DBObject) JSON.parse(json);
    Record record2 = RecordConverter.toRecord(schema, object, getClass().getClassLoader());

    assertThat(record2).isEqualTo(record1);
    assertThat(AvroHelper.toJson(schema, record2)).isEqualTo(AvroHelper.toJson(schema, record1));
  }

  @Test
  public void testTypes1() throws Exception {
    Schema schema = getSchema("schemata/types1.avsc");
    DBObject mongoObject = new BasicDBObject(ImmutableMap.of("x", 1.0, "y", 1.0));
    String mongoString = JSON.serialize(mongoObject);

    String avroJson = "{\"objectId\": \"5401bf578de2a77380c5489a\", \"bsonTimestamp1\": \"(1409385948, 1)\", \"bsonTimestamp2\": 1409385948001, \"date1\": \"2014-08-31T08:09:34.033Z\", \"date2\": 1409440174033, \"mongoString\": \"" + mongoString.replace("\"", "\\\"") + "\"}";
    Decoder decoder = DecoderFactory.get().jsonDecoder(schema, avroJson);
    SpecificDatumReader<Types1> reader = new SpecificDatumReader<Types1>(schema);
    Types1 types1 = reader.read(null, decoder);

    DBObject object = new BasicDBObject();
    object.put("_id", new ObjectId("5401bf578de2a77380c5489a"));
    object.put("bsonTimestamp1", new BSONTimestamp(1409385948, 1));
    object.put("bsonTimestamp2", new BSONTimestamp(1409385948, 1));
    object.put("date1", MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    object.put("date2", MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    object.put("mongoString", mongoObject);
    Types1 types2 = RecordConverter.toRecord(Types1.class, object);

    assertThat(types1.getObjectId().toString()).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(types2.getObjectId().toString()).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(types1.getBsonTimestamp1().toString()).isEqualTo("(1409385948, 1)");
    assertThat(types2.getBsonTimestamp1().toString()).isEqualTo("(1409385948, 1)");
    assertThat(types1.getBsonTimestamp2()).isEqualTo(1409385948001l);
    assertThat(types2.getBsonTimestamp2()).isEqualTo(1409385948001l);
    assertThat(types1.getDate1().toString()).isEqualTo("2014-08-31T08:09:34.033Z");
    assertThat(types2.getDate1().toString()).isEqualTo("2014-08-31T08:09:34.033Z");
    assertThat(types1.getDate2()).isEqualTo(1409440174033l);
    assertThat(types2.getDate2()).isEqualTo(1409440174033l);
    assertThat(types1.getMongoString().toString()).isEqualTo(mongoString);
    assertThat(types2.getMongoString().toString()).isEqualTo(mongoString);

    DBObject object1 = RecordConverter.toDbObject(types1);
    DBObject object2 = RecordConverter.toDbObject(types2);

    assertThat(object1.get("_id")).isEqualTo(new ObjectId("5401bf578de2a77380c5489a"));
    assertThat(object2.get("_id")).isEqualTo(new ObjectId("5401bf578de2a77380c5489a"));
    assertThat(object1.get("bsonTimestamp1")).isEqualTo(new BSONTimestamp(1409385948, 1));
    assertThat(object2.get("bsonTimestamp1")).isEqualTo(new BSONTimestamp(1409385948, 1));
    assertThat(object1.get("bsonTimestamp2")).isEqualTo(new BSONTimestamp(1409385948, 1));
    assertThat(object2.get("bsonTimestamp2")).isEqualTo(new BSONTimestamp(1409385948, 1));
    assertThat(object1.get("date1")).isEqualTo(
        MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    assertThat(object2.get("date1")).isEqualTo(
        MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    assertThat(object1.get("date2")).isEqualTo(
        MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    assertThat(object2.get("date2")).isEqualTo(
        MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    assertThat(object1.get("mongoString")).isEqualTo(mongoObject);
    assertThat(object2.get("mongoString")).isEqualTo(mongoObject);
  }

  @Test
  public void testTypes2() throws Exception {
    Schema schema = getSchema("schemata/types2.avsc");
    DBObject mongoObject = new BasicDBObject(ImmutableMap.of("x", 1.0, "y", 1.0));
    String mongoString = JSON.serialize(mongoObject);

    String avroJson = "{\"objectId\": \"5401bf578de2a77380c5489a\", \"bsonTimestamp1\": \"(1409385948, 1)\", \"bsonTimestamp2\": 1409385948001, \"date1\": \"2014-08-31T08:09:34.033Z\", \"date2\": 1409440174033, \"mongoString\": \"" + mongoString.replace("\"", "\\\"") + "\"}";
    Decoder decoder = DecoderFactory.get().jsonDecoder(schema, avroJson);
    SpecificDatumReader<Types2> reader = new SpecificDatumReader<Types2>(schema);
    Types2 types1 = reader.read(null, decoder);

    DBObject object = new BasicDBObject();
    object.put("_id", new ObjectId("5401bf578de2a77380c5489a"));
    object.put("bsonTimestamp1", new BSONTimestamp(1409385948, 1));
    object.put("bsonTimestamp2", new BSONTimestamp(1409385948, 1));
    object.put("date1", MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    object.put("date2", MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    object.put("mongoString", mongoObject);
    Types2 types2 = RecordConverter.toRecord(Types2.class, object);

    assertThat(types1.getObjectId().toString()).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(types2.getObjectId().toString()).isEqualTo("5401bf578de2a77380c5489a");
    assertThat(types1.getBsonTimestamp1().toString()).isEqualTo("(1409385948, 1)");
    assertThat(types2.getBsonTimestamp1().toString()).isEqualTo("(1409385948, 1)");
    assertThat(types1.getBsonTimestamp2()).isEqualTo(1409385948001l);
    assertThat(types2.getBsonTimestamp2()).isEqualTo(1409385948001l);
    assertThat(types1.getDate1().toString()).isEqualTo("2014-08-31T08:09:34.033Z");
    assertThat(types2.getDate1().toString()).isEqualTo("2014-08-31T08:09:34.033Z");
    assertThat(types1.getDate2()).isEqualTo(1409440174033l);
    assertThat(types2.getDate2()).isEqualTo(1409440174033l);
    assertThat(types1.getMongoString().toString()).isEqualTo(mongoString);
    assertThat(types2.getMongoString().toString()).isEqualTo(mongoString);

    DBObject object1 = RecordConverter.toDbObject(types1);
    DBObject object2 = RecordConverter.toDbObject(types2);

    assertThat(object1.get("_id")).isEqualTo(new ObjectId("5401bf578de2a77380c5489a"));
    assertThat(object2.get("_id")).isEqualTo(new ObjectId("5401bf578de2a77380c5489a"));
    assertThat(object1.get("bsonTimestamp1")).isEqualTo(new BSONTimestamp(1409385948, 1));
    assertThat(object2.get("bsonTimestamp1")).isEqualTo(new BSONTimestamp(1409385948, 1));
    assertThat(object1.get("bsonTimestamp2")).isEqualTo(new BSONTimestamp(1409385948, 1));
    assertThat(object2.get("bsonTimestamp2")).isEqualTo(new BSONTimestamp(1409385948, 1));
    assertThat(object1.get("date1")).isEqualTo(
        MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    assertThat(object2.get("date1")).isEqualTo(
        MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    assertThat(object1.get("date2")).isEqualTo(
        MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    assertThat(object2.get("date2")).isEqualTo(
        MongoDbTypeConverter.DATE_FORMAT.parse("2014-08-31T08:09:34.033Z"));
    assertThat(object1.get("mongoString")).isEqualTo(mongoObject);
    assertThat(object2.get("mongoString")).isEqualTo(mongoObject);
  }

  @Test
  public void testUnions() throws Exception {
    Schema schema = getSchema("schemata/unions.avsc");

    String avroJson = "{\"union1\": {\"int\": 1}, \"union2\": {\"test.Union2\": {\"union21\": {\"long\": 2}}}, \"union3\": {\"array\": [{\"boolean\": true}, {\"boolean\": false}, {\"null\": null}]}, \"union4\": {\"map\": {\"a\": {\"string\": \"A\"}, \"b\": {\"string\": \"B\"}, \"c\": {\"string\": \"C\"}}}, \"union5\": {\"null\": null}, \"union6\": {\"null\": null}}";
    Decoder decoder = DecoderFactory.get().jsonDecoder(schema, avroJson);
    GenericDatumReader<Record> reader = new GenericDatumReader<Record>(schema);
    Record record1 = reader.read(null, decoder);

    String mongoJson = "{\"union1\": 1, \"union2\": {\"union21\": 2}, \"union3\": [true, false, null], \"union4\": {\"a\": \"A\", \"b\": \"B\", \"c\": \"C\"}, \"union5\": null, \"union6\": null}";
    DBObject object = (DBObject) JSON.parse(mongoJson);
    Record record2 = RecordConverter.toRecord(schema, object, getClass().getClassLoader());

    assertThat(record2).isEqualTo(record1);
    assertThat(AvroHelper.toJson(schema, record2)).isEqualTo(AvroHelper.toJson(schema, record1));
  }

  private Schema getSchema(String name) throws IOException {
    InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(name);
    return new Schema.Parser().parse(schemaStream);
  }
}
