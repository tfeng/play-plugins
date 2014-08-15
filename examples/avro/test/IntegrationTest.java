import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.net.URI;
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

import controllers.protocols.Example;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:spring/*.xml"})
public class IntegrationTest {

  @Test
  public void testSendBinaryRequest() {
    running(testServer(3333), () -> {
      try {
        HttpTransceiver transceiver = new HttpTransceiver(new URL("http://localhost:3333"));
        Example example = SpecificRequestor.getClient(Example.class, transceiver);
        assertThat(example.echo("Test Message")).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testSendJsonRequest() {
    running(testServer(3333), () -> {
      try {
        Protocol protocol = Example.PROTOCOL;
        Schema schema = protocol.getMessages().get("echo").getRequest();

        URI uri = new URL("http://localhost:3333").toURI();
        GenericRequestor client =
            new GenericRequestor(protocol, Ipc.createTransceiver(uri));
        GenericDatumReader<Object> reader = new GenericDatumReader<Object>(schema);
        Object request = reader.read(null, DecoderFactory.get().jsonDecoder(schema,
            "{\"message\": \"Test Message\"}"));
        Object response = client.request("echo", request);
        assertThat(response).isEqualTo("Test Message");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
