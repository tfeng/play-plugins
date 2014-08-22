package me.tfeng.play.avro.d2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.Transceiver;

public class AvroD2Transceiver extends Transceiver {

  private final AvroD2Client client;

  private final HttpTransceiver transceiver;

  public AvroD2Transceiver(AvroD2Client client) {
    this.client = client;
    transceiver = new HttpTransceiver(client.getNextServerUrl());
  }

  @Override
  public String getRemoteName() throws IOException {
    return AvroD2Helper.getUri(client.getProtocol()).toString();
  }

  @Override
  public List<ByteBuffer> readBuffers() throws IOException {
    return transceiver.readBuffers();
  }

  @Override
  public void writeBuffers(List<ByteBuffer> buffers) throws IOException {
    transceiver.writeBuffers(buffers);
  }
}
