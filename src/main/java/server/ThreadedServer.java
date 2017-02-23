package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static common.Settings.dateTimeFormatter;

public class ThreadedServer {

  public static final int PORT = 1978;

  public static class XmlFile {
    String fileName;
    String xml;
    String SHA3;
    LocalDateTime modificationDate;
  }

  private List<XmlFile> storedXmls = new LinkedList<>();

  public synchronized String getList() {
    StringBuilder sb = new StringBuilder();
    for (XmlFile xml : storedXmls)
      sb.append(xml.fileName).append(" ")
          .append(xml.modificationDate.format(dateTimeFormatter)).append(" ")
          .append(xml.SHA3).append("\n");
    sb.append('\n');
    return sb.toString();
  }

  public synchronized void updateXml(String fileName, String xmlString) {
    for (XmlFile xml : storedXmls)
      if (xml.fileName.equals(fileName)) {
        // Create backup version
        XmlFile backupXml = new XmlFile();
        backupXml.fileName = xml.fileName + xml.modificationDate.format(dateTimeFormatter);
        backupXml.xml = xml.xml;
        backupXml.SHA3 = xml.SHA3;
        backupXml.modificationDate = xml.modificationDate;

        storedXmls.add(backupXml);

        // Update element
        xml.xml = xmlString;
        xml.modificationDate = LocalDateTime.now();
        xml.SHA3 = calculateSha256(xml.xml);

        return;
      }

    // XmlFile not found, create new one
    XmlFile newXml = new XmlFile();
    newXml.fileName = fileName;
    newXml.xml = xmlString;
    newXml.modificationDate = LocalDateTime.now();
    newXml.SHA3 = calculateSha256(newXml.xml);
    storedXmls.add(newXml);
  }

  public synchronized String getFile(String name) {
    for (XmlFile xml : storedXmls) {
      if (xml.fileName.equals(name))
        return xml.xml;
    }

    return null;
  }

  // Returns HEX representation of SHA-256
  public String calculateSha256(String text) {
    MessageDigest md;

    try {
      md = MessageDigest.getInstance("SHA-256");
    }
    catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      throw new RuntimeException("No SHA-256 algorithm");
    }

    md.update(text.getBytes(StandardCharsets.UTF_8));
    byte[] bytes = md.digest();

    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02X", b));
    }

    return sb.toString();
  }

  public static void main(String args[]) {
    ThreadedServer server = new ThreadedServer();
    ServerSocket serverSocket;

    try {
      serverSocket = new ServerSocket(PORT);
    }
    catch (IOException e) {
      e.printStackTrace();
      return;
    }
    while (true) {
      try {
        Socket socket = serverSocket.accept();
        System.err.println("New client connection.");
        new ServerThread(socket, server).start();
      }
      catch (IOException e) {
        System.out.println("I/O error: " + e);
      }
    }
  }
}
