package Tracker;

import Peers.Message;
import Peers.MessageType;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ConcurrentHashMap<String, PeerInfo> peers;
    private Boolean clientConnected;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, PeerInfo> peers, ConcurrentHashMap<String, PeerInfo> connectedPeers) {
        this.clientSocket = socket;
        this.peers = peers;
    }

    @Override
    public void run() {
        try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {
            clientConnected = true;

            // Accepting peer messages until peer sends MessageType.EXIT Message
            while (clientConnected) {
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

        // handle registration form data
        String[] details = msg.getContent().split(":");
        String username = details[0];
        String password = details[1];

        if (peers.containsKey(username)) {
            oos.writeObject(new Message(MessageType.ERROR, "Peer already registered"));

        } else {

            // Instantiate new PeerInfo object for the new registrant
            PeerInfo newPeer = new PeerInfo(msg.getFiles(), username, password);

            // Insert new registrant peer to HashMap
            peers.put(username, newPeer);
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

    /**
     * Manages peer login:
     * - Splits and extracts username and password from the message.
     * - Authenticates credentials against stored data.
     * - On successful authentication:
     * - Generates and assigns a new session token.
     * - Sends a success message with the token.
     * - On failure, sends an error message.
     * - Handles session management and ensures secure access.
     */
    private void performLogin(Message msg, ObjectOutputStream oos) throws IOException {
        // Extract credentials from the message content
        String[] credentials = msg.getContent().split(":");
        String username = credentials[0];
        String password = credentials[1];

        // Check if the provided credentials match a registered user
        if (authenticate(username, password)) {
            // Generate a new session token for the login session
            String token = generateToken();

            // Retrieve the peer's information and update its session token
            PeerInfo peer = peers.get(username);
            peer.setToken(token);  // Store the new session token in the peer's information

            // Gather IP address and port information
            String ipAddress = clientSocket.getInetAddress().getHostAddress();
            int port = clientSocket.getPort();

            // Inform the peer of the successful login and send the session token
            Message reply = new Message(MessageType.LOGIN_SUCCESS, "Login successful: Token=" + token);
            reply.setContent("IP Address: " + ipAddress + ", Port: " + port + ", Token: " + token);
            oos.writeObject(reply);

        } else {
            // Send an error message if the login fails
            oos.writeObject(new Message(MessageType.ERROR, "Login failed"));
        }
    }

    private void performLogout(Message msg, ObjectOutputStream oos) throws IOException {
        String token = msg.getToken();
        boolean found = false;

        for (PeerInfo peer : peers.values()) {

            if (token.equals(peer.getToken())) {
                peer.setToken(null);  // Invalidate the token
                oos.writeObject(new Message(MessageType.RESPONSE, "Logout successful"));
                found = true;
                clientConnected = false;
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
        PeerInfo peer = peers.get(username);
        return peer != null && peer.getPassword().equals(password);
    }

    //TODO (Not ready yet)
    private void handleDetails(Message msg, ObjectOutputStream oos) throws IOException {
        String requestedFile = msg.getContent(); // Name of the file requested
        List<PeerInfo> peersWithFile = new ArrayList<>();

        // Iterate over the collection of peers and add those with the requested file to the list
        for (PeerInfo peer : peers.values()) {
            if (peer.getFiles().contains(requestedFile)) {
                peersWithFile.add(peer);
            }
        }

        if (peersWithFile.isEmpty()) {
            oos.writeObject(new Message(MessageType.ERROR, "No peers have the file"));
        } else {
            // Prepare a detailed response
            StringBuilder details = new StringBuilder();
            for (PeerInfo peer : peersWithFile) {
                details.append(peer.getUsername())
                        .append(", IP: ").append(peer.getIpAddress())
                        .append(", Port: ").append(peer.getPort())
                        .append(", Downloads: ").append(peer.getCountDownloads())
                        .append(", Failures: ").append(peer.getCountFailures())
                        .append("\n");
            }
            oos.writeObject(new Message(MessageType.RESPONSE, details.toString()));
        }
    }

    /**
     * Handles checking if a peer is currently active based on a token provided in the message.
     * - Retrieves the peer from the peers list using the token from the message.
     * - Checks if the peer's last activity time is within the allowed timeout period.
     * - Updates the peer's last heartbeat time if they are still active.
     * - Sends a response back indicating whether the peer is currently active or inactive.
     */
    private void handleCheckActive(Message msg, ObjectOutputStream oos) throws IOException {
        // Extract the token from the message to identify the peer.
        String token = msg.getContent();
        PeerInfo peer = null;

        // Manually search for the peer with the given token.
        for (PeerInfo p : peers.values()) {
            if (token.equals(p.getToken())) {
                peer = p;
                break;
            }
        }

        if (peer != null) {
            long currentTime = System.currentTimeMillis();  // Get current time.
            long lastActiveTime = peer.getLastHeartbeat();  // Get last recorded active time.
            long timeout = 60000;  // Timeout value set to 60 seconds.

            // Check if the last activity is within the timeout period.
            if ((currentTime - lastActiveTime) < timeout) {
                peer.setLastHeartbeat(currentTime);  // Update last active time.
                oos.writeObject(new Message(MessageType.RESPONSE, "Peer is active"));
            } else {
                oos.writeObject(new Message(MessageType.ERROR, "Peer is inactive"));
            }
        } else {
            oos.writeObject(new Message(MessageType.ERROR, "Peer not found"));  // No peer found with the given token.
        }
    }
}
