package Tracker;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class Tracker {
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, PeerInfo> peers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, PeerInfo> connectedPeers = new ConcurrentHashMap<>();


    public Tracker(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Tracker running on port " + port);
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, peers, connectedPeers)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Tracker tracker = new Tracker(1111);
        tracker.start();
    }
}