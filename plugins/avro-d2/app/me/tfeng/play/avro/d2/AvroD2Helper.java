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

public class AvroD2Helper {

  public static final String SCHEME = "avlb";

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

  public static Protocol getProtocol(Class<?> interfaceClass) {
    try {
      return (Protocol) interfaceClass.getField("PROTOCOL").get(null);
    } catch (Exception e) {
      throw new RuntimeException("Unable to get protocol for interface class "
          + interfaceClass.getName());
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
