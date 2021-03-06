package client;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static common.Settings.dateTimeFormatter;

public class Client {

  private Socket socket;
  private InputStream serverInput;
  private BufferedReader serverReader;
  private DataOutputStream serverOutput;

  public Client(Socket socket) throws IOException {
    this.socket = socket;
    serverInput = socket.getInputStream();
    serverReader = new BufferedReader(new InputStreamReader(serverInput));
    serverOutput = new DataOutputStream(socket.getOutputStream());
  }

  public void close() {
    if (socket != null)
      try {
        socket.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
  }

  public void uploadFile(String filename) throws IOException, IllegalArgumentException {
    System.err.print("Uploading file '" + filename + "'... ");
    serverOutput.writeBytes(
        "SEND " + filename + "\n" + FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8) + "\n\0\n");
    String response = readMultilineMessage();
    if (response.startsWith("OK")) {
      System.err.println("done");
      return;
    }
    System.err.println("error:\n" + response);
    throw new IllegalArgumentException(response.isEmpty() ? "Server did not respond." : response);
  }

  public void downloadFile(String filename) throws IOException {
    System.err.print("Downloading file '" + filename + "'... ");
    serverOutput.writeBytes("GET " + filename + '\n');
    serverOutput.flush();

    String xml = readMultilineMessage();
    System.err.println("File received:\n " + xml);
    FileUtils.writeStringToFile(new File(filename), xml, StandardCharsets.UTF_8);
    System.err.println("done");
  }

  private String readMultilineMessage() throws IOException {
    StringBuilder xmlBuilder = new StringBuilder();
    while (true) {
      String line = serverReader.readLine();
      if (line == null || line.equals("\0"))
        break;
      xmlBuilder.append(line);
      xmlBuilder.append('\n');
    }
    return xmlBuilder.toString();
  }

  public Set<FileListEntry> getFilesList() throws IOException {
    System.err.print("Getting files list... ");
    serverOutput.writeBytes("LIST\n");
    serverOutput.flush();
    Set<FileListEntry> list = new HashSet<>();
    while (true) {
      String line = serverReader.readLine();
      if (line == null || line.isEmpty())
        break;
      String[] data = line.split(" ");
      String filename = data[0];
      LocalDateTime modificationDate = LocalDateTime.parse(data[1], dateTimeFormatter);
      String sha = data[2];
      list.add(new FileListEntry(filename, sha, modificationDate));
    }
    System.err.println("done");
    return list;
  }
}
