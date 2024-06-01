package Peers;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class peerServer implements Runnable {

    private String shared_dir;
    private int port;
    private Peer peer;
    ServerSocket server;
    private ArrayList<RequestInfo> requests = new ArrayList<>();

    public peerServer(ServerSocket server, String shared_dir, Peer peer) {
        this.server = server;
        this.shared_dir = shared_dir;
        this.peer = peer;
    }

    public void run() {
        try {

            Thread daemonThread = new Thread(() -> {
                // Daemon thread runs always in the background
                while(true) {
                    synchronized (requests) {

                        try {
                            // Wait() until a download request is sent
                            requests.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    long startTime = System.currentTimeMillis();

                    while (System.currentTimeMillis() - startTime < 200){}

                    synchronized (requests){

                        // Start the thread to initiate Collaborative download.
                        new Thread(new ColabDownloadHandler(requests, this.shared_dir, this.peer)).start();

                        // Wait for ColabDownloadHandler to copy requests' objects references
                        try {
                            requests.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        //  -------------- Copy procedure ended --------------
                        requests.clear();
                    }
                }
            });

            daemonThread.setDaemon(true);
            daemonThread.start();
            while (true) {
                Socket client = this.server.accept();
                new Thread(new ClientHandler(client, this.shared_dir, this.requests)).start();
            }

        } catch (IOException e) {
            System.out.println("Server is closed...");
        }

    }
}


class RequestInfo {
    public Message msg;
    public ObjectOutputStream oos;
    public ObjectInputStream ois;
    public String peerUsername;

    public RequestInfo(Message msg, ObjectOutputStream oos, ObjectInputStream ois, String peerUsername) {
        this.msg = msg;
        this.oos = oos;
        this.ois = ois;
        this.peerUsername = peerUsername;
    }
}

