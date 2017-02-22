package server;

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
        String xml = server.getFile(fileName);

        if(xml == null)
            out.writeChars("ERROR No such file");
        else
            out.writeChars(xml);
    }

    private void processSend(DataOutputStream out, String fileName, BufferedReader brinp) throws IOException {

        StringBuilder xmlBuilder = new StringBuilder();

        String line = brinp.readLine();
        while(line != null)
        {
            xmlBuilder.append(line);
            line = brinp.readLine();
        }

        String xml = xmlBuilder.toString();
        // TODO validate XML


        server.updateXml(fileName, xml);
        out.writeChars("OK");
    }

    public void run() {
        InputStream inp;
        BufferedReader brinp;
        DataOutputStream out;
        try {
            inp = socket.getInputStream();
            brinp = new BufferedReader(new InputStreamReader(inp));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            return;
        }
        String line;
        try {
            line = brinp.readLine();
            if (line == null){
                System.out.println("Error empty message");
                socket.close();
                return;
            }

            if(line.equals("LIST")) {
                out.writeChars(server.getList());
                out.flush();
                socket.close();
                return;
            }

            if(line.startsWith("SEND ")) {
                String fileName = line.substring(5);
                processSend(out, fileName, brinp);
                out.flush();
                socket.close();
                return;
            }

            if(line.startsWith("GET ")) {
                String fileName = line.substring(4);
                processGet(out, fileName);
                out.flush();
                socket.close();
                return;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}