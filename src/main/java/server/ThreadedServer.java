package server;

import org.bouncycastle.jce.provider.JCEMac;

import java.net.*;
import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ThreadedServer {

    static final int PORT = 1978;

    public static class XML
    {
        String fileName;
        String xml;
        String SHA3;
        Date modificationDate;
    }

    private List<XML> storedXmls = new LinkedList<>();

    public synchronized String getList()
    {
        StringBuilder sb = new StringBuilder();
        for(XML xml: storedXmls)
            sb.append(xml.fileName).append(" ").append(xml.modificationDate).append(" ").append(xml.SHA3).append("\n");

        return sb.toString();
    }

    public synchronized void updateXml(String fileName, String xmlString)
    {
        for(XML xml : storedXmls)
            if(xml.fileName.equals(fileName))
            {
                // Create backup version
                XML el = new XML();
                el.fileName = xml.fileName + xml.modificationDate.toString();
                el.xml = xml.xml;
                el.SHA3 = xml.SHA3;
                el.modificationDate = xml.modificationDate;

                storedXmls.add(el);

                // Update element
                xml.xml = xmlString;
                xml.modificationDate = new Date();
                xml.SHA3 = calculateSha3(xml.xml);

                return;
            }

        // XML not found, create new one
        XML newXml = new XML();
        newXml.fileName = fileName;
        newXml.xml = xmlString;
        newXml.modificationDate = new Date();
        newXml.SHA3 = calculateSha3(newXml.xml);
        storedXmls.add(newXml);
    }

    public synchronized String getFile(String name)
    {
        for(XML xml : storedXmls)
        {
            if(xml.fileName.equals(name))
                return xml.xml;
        }

        return null;
    }

    public String calculateSha3(String text)
    {
        // TODO IMPLEMENT
        // http://www.bouncycastle.org/ ?

        return "0";
    }

    public static void main(String args[]) {
        ThreadedServer server = new ThreadedServer();
        ServerSocket serverSocket;
        Socket socket = null;

        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            // new thread for a client
            new ServerThread(socket, server).start();
        }
    }
}
