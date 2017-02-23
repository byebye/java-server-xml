package server;

import org.xml.sax.SAXException;
import server.ThreadedServer.XmlFile;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.net.Socket;

public class ServerThread extends Thread {
  private Socket socket;
  private ThreadedServer server;

  public ServerThread(Socket clientSocket, ThreadedServer server) {
    this.socket = clientSocket;
    this.server = server;
  }

  private void processGet(DataOutputStream out, String fileName) throws IOException {
    XmlFile xml = server.getFile(fileName);

    if (xml == null)
      out.writeBytes("ERROR No such file");
    else
      out.writeBytes(xml.xml + "\0\n");
  }

  private void processSend(DataOutputStream out, String fileName, BufferedReader brinp) throws IOException {

    StringBuilder xmlBuilder = new StringBuilder();

    while (true) {
      String line = brinp.readLine();
      if (line == null || line.equals("\0"))
        break;
      xmlBuilder.append(line);
      xmlBuilder.append('\n');
    }

    String xml = xmlBuilder.toString();
    System.err.println("File received:\n " + xml);
    if(!validateSchema(xml))
    {
        out.writeBytes("ERROR Schema Validation Failed\n");
        return;
    }
    server.updateXml(fileName, xml);
    out.writeBytes("OK\n");
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
        System.err.println("Waiting for message... ");
        String line = brinp.readLine();
        if (line == null) {
          throw new IllegalArgumentException("Empty message.");
        }
        else if (line.startsWith("LIST")) {
          System.err.print("List files...");
          out.writeBytes(server.getList());
          out.flush();
          System.err.println("done");
        }
        else if (line.startsWith("SEND ")) {
          String fileName = line.substring(5);
          System.err.print("Receive a file '" + fileName + "' from client... ");
          processSend(out, fileName, brinp);
          out.flush();
          System.err.println("done");
        }
        else if (line.startsWith("GET ")) {
          String fileName = line.substring(4);
          System.err.print("Send the file '" + fileName + "' to client... ");
          processGet(out, fileName);
          out.flush();
          System.err.println("done");
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

  public boolean validateSchema(String xml)
  {
      Unmarshaller unmarshaller ;
      common.xml.ObjectFactory factory = new common.xml.ObjectFactory();
      try {
          JAXBContext context = JAXBContext.newInstance(factory.getClass());
          unmarshaller = context.createUnmarshaller();
      } catch (JAXBException e) {
          e.printStackTrace();
          return false;
      }

      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema;
      try {
          schema = schemaFactory.newSchema(new File("src/main/resources/schema.xsd"));
      } catch (SAXException e) {
          e.printStackTrace();
          return false;
      }

      unmarshaller.setSchema(schema);
      StringReader reader = new StringReader(xml);
      try {
          unmarshaller.unmarshal(reader);
      } catch (JAXBException e) {
          return false;
      }
      return true;
  }
}