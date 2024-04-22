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

            // Accepting peer messages until peer sends "Exit" message
            //TODO handle peer "Exit" message
            while(true) {
                Object obj = ois.readObject();
                if (obj instanceof Message) {
                    Message msg = (Message) obj;
                    handleMessage(msg, oos);
                }
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
                registerPeer(msg, oos);
                break;
            case LIST_FILES:
                listFiles(oos);
                break;
            case LOGIN:
                performLogin(msg, oos);
                break;
            case LOGOUT:
                performLogout(msg, oos);
                break;
            default:
                oos.writeObject(new Message(MessageType.ERROR, "Unknown command"));
        }
    }
    private void registerPeer(Message msg, ObjectOutputStream oos) throws IOException {
        if (peers.containsKey(msg.getPeerId())) {
            oos.writeObject(new Message(MessageType.ERROR, "Peer already registered"));
        } else {
            String[] details = msg.getContent().split(":");
            String username = details[0];
            String password = details[1];
            PeerInfo newPeer = new PeerInfo(msg.getPeerId(), msg.getFiles(), username, password);
            peers.put(msg.getPeerId(), newPeer);
            oos.writeObject(new Message(MessageType.RESPONSE, "Registered successfully"));
        }
    }

    private void listFiles(ObjectOutputStream oos) throws IOException {
        HashSet<String> allFiles = new HashSet<>();
        for (PeerInfo peerInfo : peers.values()) {
            allFiles.addAll(peerInfo.getFiles());
        }
        oos.writeObject(new Message(MessageType.RESPONSE, new ArrayList<>(allFiles).toString()));
    }

    private void performLogin(Message msg, ObjectOutputStream oos) throws IOException {
        String[] credentials = msg.getContent().split(":");
        String username = credentials[0];
        String password = credentials[1];
        if (authenticate(username, password)) {
            String token = generateToken();
            peers.get(msg.getPeerId()).setToken(token);  // Store the token
            oos.writeObject(new Message(MessageType.LOGIN_SUCCESS, token));
        } else {
            oos.writeObject(new Message(MessageType.ERROR, "Login failed"));
        }
    }

    private void performLogout(Message msg, ObjectOutputStream oos) throws IOException {
        String token = msg.getContent();
        boolean found = false;

        for (PeerInfo peer : peers.values()) {
            if (token.equals(peer.getToken())) {
                peer.setToken(null);  // Invalidate the token
                oos.writeObject(new Message(MessageType.RESPONSE, "Logout successful"));
                found = true;
                break;
            }
        }

        if (!found) {
            oos.writeObject(new Message(MessageType.ERROR, "Invalid token or already logged out"));
        }
    }
    private String generateToken() {
       // Implementation for generating a unique session token
      return Long.toHexString(Double.doubleToLongBits(Math.random()));
    }
    private boolean authenticate(String username, String password) {
        for (PeerInfo peer : peers.values()) {
            if (peer.getUsername().equals(username) && peer.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }
}
