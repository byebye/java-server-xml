package server;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import static common.Settings.dateTimeFormatter;

public class ThreadedServer {

  public static final DateTimeFormatter fileDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  public static final int PORT = 1978;

  public static class XmlFile {
    String fileName;
    String xml;
    String SHA3;
    LocalDateTime modificationDate;
  }

  private Map<String, XmlFile> storedXmls = new TreeMap<>();

  public synchronized String getList() {
    StringBuilder sb = new StringBuilder();
    for (XmlFile xml : storedXmls.values())
      sb.append(xml.fileName).append(" ")
          .append(xml.modificationDate.format(dateTimeFormatter)).append(" ")
          .append(xml.SHA3).append("\n");
    sb.append('\n');
    return sb.toString();
  }

  public synchronized void updateXml(String fileName, String xmlString) {
    String sha = calculateSha256(xmlString);

    if (storedXmls.containsKey(fileName)) {
      XmlFile xml = storedXmls.get(fileName);
      if (xml.SHA3.equals(sha)) // Same file, do not create backup
        return;
      // Create backup version
      XmlFile backupXml = new XmlFile();
      backupXml.fileName = FilenameUtils.removeExtension(xml.fileName)
                           + "." + xml.modificationDate.format(fileDateTimeFormatter) + ".xml";
      backupXml.xml = xml.xml;
      backupXml.SHA3 = xml.SHA3;
      backupXml.modificationDate = xml.modificationDate;

      storedXmls.put(backupXml.fileName, backupXml);

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
    newXml.SHA3 = sha;
    storedXmls.put(newXml.fileName, newXml);
  }

  public synchronized XmlFile getFile(String name) {
    return storedXmls.get(name);
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
