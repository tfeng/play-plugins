package me.tfeng.play.avro.d2;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Protocol;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import play.Logger;
import play.Logger.ALogger;

public class AvroD2Client implements Watcher {

  private static final ALogger LOG = Logger.of(AvroD2Client.class);

  private final Protocol protocol;
  private final List<URL> serverUrls = new ArrayList<>();
  private final ZooKeeper zk;

  public AvroD2Client(ZooKeeper zk, Protocol protocol) throws KeeperException,
      InterruptedException {
    this.zk = zk;
    this.protocol = protocol;
    refresh();
  }

  @Override
  public void process(WatchedEvent event) {
    try {
      refresh();
    } catch (Exception e) {
      LOG.error("Unable to get server URL from path " + AvroD2Helper.getZkPath(protocol));
    }
  }

  public void refresh() throws KeeperException, InterruptedException {
    List<String> children = zk.getChildren(AvroD2Helper.getZkPath(protocol), this);
    serverUrls.clear();
    for (String child : children) {
      try {
        byte[] data = zk.getData(child, false, null);
        String serverUrl = new String(data, Charset.forName("utf8"));
        serverUrls.add(new URL(serverUrl));
      } catch (Exception e) {
        LOG.error("Unable to get server URL from node " + child);
      }
    }
  }
}
