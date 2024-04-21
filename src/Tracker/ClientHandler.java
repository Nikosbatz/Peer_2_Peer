package Tracker;

import Peers.Message;
import Peers.MessageType;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ConcurrentHashMap<String, PeerInfo> peers;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, PeerInfo> peers) {
        this.clientSocket = socket;
        this.peers = peers;
    }

    @Override
    public void run() {
        try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {

            Object obj = ois.readObject();
            if (obj instanceof Message) {
                Message msg = (Message) obj;
                handleMessage(msg, oos);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handleMessage(Message msg, ObjectOutputStream oos) throws IOException {
        switch (msg.getType()) {
            case REGISTER:
                peers.put(msg.getPeerId(), new PeerInfo(msg.getPeerId(), msg.getFiles()));
                oos.writeObject(new Message(MessageType.RESPONSE, "Registered successfully"));
                break;
            case LIST_FILES:
                HashSet<String> allFiles = new HashSet<>();
                for (PeerInfo peerInfo : peers.values()) {
                    allFiles.addAll(peerInfo.getFiles());
                }
                oos.writeObject(new Message(MessageType.RESPONSE, new ArrayList<>(allFiles).toString()));
                break;
            case LOGIN:
                // Process login
                break;
            case LOGOUT:
                // Process logout
                break;
            default:
                oos.writeObject(new Message(MessageType.ERROR, "Unknown command"));
        }
    }
}
