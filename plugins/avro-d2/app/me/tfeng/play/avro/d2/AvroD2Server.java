package me.tfeng.play.avro.d2;

import java.net.URL;

import org.apache.avro.Protocol;
import org.apache.zookeeper.ZooKeeper;

import play.Logger;
import play.Logger.ALogger;

public class AvroD2Server {

  private static final ALogger LOG = Logger.of(AvroD2Server.class);

  protected String nodePath;
  protected final Protocol protocol;
  protected final URL serverUrl;
  protected final ZooKeeper zk;

  public AvroD2Server(ZooKeeper zk, Protocol protocol, URL serverUrl) {
    this.zk = zk;
    this.protocol = protocol;
    this.serverUrl = serverUrl;
    register();
  }

  public void close() {
    try {
      if (nodePath != null) {
        LOG.info("Closing server " + serverUrl);
        zk.delete(nodePath, -1);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to close server at " + AvroD2Helper.getUri(protocol), e);
    }
  }

  public void register() {
    close();

    LOG.info("Registering server for " + AvroD2Helper.getUri(protocol) + " at " + serverUrl);
    try {
      nodePath = AvroD2Helper.createProtocolNode(zk, protocol, serverUrl);
    } catch (Exception e) {
      throw new RuntimeException("Unable to register server at " + AvroD2Helper.getUri(protocol),
          e);
    }
  }
}
