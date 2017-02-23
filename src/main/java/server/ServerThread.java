package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerThread extends Thread {
  private Socket socket;
  private ThreadedServer server;

  public ServerThread(Socket clientSocket, ThreadedServer server) {
    this.socket = clientSocket;
    this.server = server;
  }

  private void processGet(DataOutputStream out, String fileName) throws IOException {
    String xml = server.getFile(fileName);

    if (xml == null)
      out.writeBytes("ERROR No such file");
    else
      out.writeBytes(xml);
  }

  private void processSend(DataOutputStream out, String fileName, BufferedReader brinp) throws IOException {

    StringBuilder xmlBuilder = new StringBuilder();

    String line = brinp.readLine();
    while (line != null) {
      xmlBuilder.append(line);
      line = brinp.readLine();
    }

    String xml = xmlBuilder.toString();
    // TODO validate XmlFile

    server.updateXml(fileName, xml);
    out.writeBytes("OK");
  }

  public void run() {
    InputStream inp;
    BufferedReader brinp;
    DataOutputStream out;
    try {
      inp = socket.getInputStream();
      brinp = new BufferedReader(new InputStreamReader(inp));
      out = new DataOutputStream(socket.getOutputStream());
    }
    catch (IOException e) {
      e.printStackTrace();
      return;
    }
    while (!interrupted()) {
      try {
        System.err.println("Waiting for message...");
        String line = brinp.readLine();
        if (line == null) {
          throw new IllegalArgumentException("Empty message.");
        }
        else if (line.startsWith("LIST")) {
          System.err.print("List files...");
          out.writeBytes(server.getList());
          out.flush();
          System.err.println(" done");
        }
        else if (line.startsWith("SEND ")) {
          String fileName = line.substring(5);
          System.err.print("Receive a file '" + fileName + "' from client...");
          processSend(out, fileName, brinp);
          out.flush();
          System.err.println(" done");
        }
        else if (line.startsWith("GET ")) {
          String fileName = line.substring(4);
          System.err.print("Send the file '" + fileName + "' to client...");
          processGet(out, fileName);
          out.flush();
          System.err.println(" done");
        }
        else {
          throw new IllegalArgumentException("Unknown operation: '" + line + "'");
        }
      }
      catch (IOException e) {
        e.printStackTrace();
        break;
      }
    }
    if (socket != null && socket.isConnected())
      try {
        System.err.println("Disconnecting from client.");
        socket.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
  }
}