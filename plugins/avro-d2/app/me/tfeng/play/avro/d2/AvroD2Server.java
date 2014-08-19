package me.tfeng.play.avro.d2;

import java.net.URL;

import org.apache.avro.Protocol;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import play.Logger;
import play.Logger.ALogger;

public class AvroD2Server {

  private static final ALogger LOG = Logger.of(AvroD2Server.class);

  protected String nodePath;
  protected final Protocol protocol;
  protected final URL serverUrl;
  protected final ZooKeeper zk;

  public AvroD2Server(ZooKeeper zk, Protocol protocol, URL serverUrl) throws KeeperException,
      InterruptedException {
    this.zk = zk;
    this.protocol = protocol;
    this.serverUrl = serverUrl;

    LOG.info("Registering server for " + AvroD2Helper.getUri(protocol) + " at " + serverUrl);
    register();
  }

  protected void register() throws KeeperException, InterruptedException {
    if (nodePath != null) {
      zk.delete(nodePath, -1);
    }
    AvroD2Helper.createProtocolNode(zk, protocol, serverUrl);
  }
}
