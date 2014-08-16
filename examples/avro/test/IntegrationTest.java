import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.generic.GenericRequestor;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableList;

import controllers.protocols.Example;
import controllers.protocols.Point;
import controllers.protocols.Points;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:spring/*.xml"})
public class IntegrationTest {

  @Test
  public void testExampleBinaryRequest() {
    running(testServer(3333), () -> {
      try {
        HttpTransceiver transceiver = new HttpTransceiver(new URL("http://localhost:3333/example"));
        Example example = SpecificRequestor.getClient(Example.class, transceiver);
        assertThat(example.echo("Test Message")).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testExampleJsonRequest() {
    running(testServer(3333), () -> {
      try {
        Object response =
            sendJsonRequest("http://localhost:3333/example", Example.PROTOCOL, "echo",
                "{\"message\": \"Test Message\"}");
        assertThat(response).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testPointsBinaryRequest() {
    running(testServer(3333), () -> {
      try {
        HttpTransceiver transceiver = new HttpTransceiver(new URL("http://localhost:3333/points"));
        Points points = SpecificRequestor.getClient(Points.class, transceiver);
        Point center = Point.newBuilder().setX(0.0).setY(0.0).build();

        // []
        assertThat(points.getNearestPoints(center, 1)).isEqualTo(ImmutableList.of());

        // [one]
        Point one = Point.newBuilder().setX(1.0).setY(1.0).build();
        points.addPoint(one);
        assertThat(points.getNearestPoints(center, 1)).isEqualTo(ImmutableList.of(one));
        assertThat(points.getNearestPoints(center, 2)).isEqualTo(ImmutableList.of(one));

        // [one, five]
        Point five = Point.newBuilder().setX(5.0).setY(5.0).build();
        points.addPoint(five);
        assertThat(points.getNearestPoints(center, 1)).isEqualTo(ImmutableList.of(one));
        assertThat(points.getNearestPoints(center, 2)).isEqualTo(ImmutableList.of(one, five));
        assertThat(points.getNearestPoints(center, 3)).isEqualTo(ImmutableList.of(one, five));

        // [one, five, five]
        points.addPoint(five);
        assertThat(points.getNearestPoints(center, 1)).isEqualTo(ImmutableList.of(one));
        assertThat(points.getNearestPoints(center, 2)).isEqualTo(ImmutableList.of(one, five));
        assertThat(points.getNearestPoints(center, 3)).isEqualTo(ImmutableList.of(one, five, five));
        assertThat(points.getNearestPoints(center, 4)).isEqualTo(ImmutableList.of(one, five, five));

        // []
        points.clear();
        assertThat(points.getNearestPoints(center, 1)).isEqualTo(ImmutableList.of());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testPointsJsonRequest() {
    running(testServer(3333), () -> {
      try {
        String url = "http://localhost:3333/points";
        Object response;

        // []
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response).isEqualTo(ImmutableList.of());

        // [one]
        Point one = Point.newBuilder().setX(1.0).setY(1.0).build();
        sendJsonRequest(url, Points.PROTOCOL, "addPoint", "{\"point\": {\"x\": 1.0, \"y\": 1.0}}");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one).toString());
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 2}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one).toString());

        // [one, five]
        Point five = Point.newBuilder().setX(5.0).setY(5.0).build();
        sendJsonRequest(url, Points.PROTOCOL, "addPoint", "{\"point\": {\"x\": 5.0, \"y\": 5.0}}");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one).toString());
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 2}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one, five).toString());
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 3}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one, five).toString());

        // [one, five, five]
        sendJsonRequest(url, Points.PROTOCOL, "addPoint", "{\"point\": {\"x\": 5.0, \"y\": 5.0}}");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one).toString());
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 2}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one, five).toString());
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 3}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one, five, five).toString());
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 4}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of(one, five, five).toString());

        // []
        sendJsonRequest(url, Points.PROTOCOL, "clear", "");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString()).isEqualTo(ImmutableList.of().toString());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private Object sendJsonRequest(String url, Protocol protocol, String message, String data)
      throws URISyntaxException, IOException {
    URI uri = new URL(url).toURI();
    Schema schema = protocol.getMessages().get(message).getRequest();
    GenericRequestor client = new GenericRequestor(protocol, Ipc.createTransceiver(uri));
    GenericDatumReader<Object> reader = new GenericDatumReader<Object>(schema);
    Object request = reader.read(null, DecoderFactory.get().jsonDecoder(schema, data));
    return client.request(message, request);
  }
}
