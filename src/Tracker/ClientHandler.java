package Tracker;

import Peers.Message;
import Peers.MessageType;


import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ConcurrentHashMap<String, PeerInfo> peers;

    private ConcurrentHashMap<String, PeerInfo> connectedPeers;
    private Boolean clientConnected;


    public ClientHandler(Socket socket, ConcurrentHashMap<String, PeerInfo> peers, ConcurrentHashMap<String, PeerInfo> connectedPeers) {
        this.clientSocket = socket;
        this.peers = peers;
        this.connectedPeers = connectedPeers;
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
        System.out.println("Message type: " + msg.getType());
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
            case INFORM:
                informationFromPeer(msg, oos);
                break;
            case DETAILS:
                handleDetails(msg,oos);
                break;
            case NOTIFY_FAIL:
            case NOTIFY_SUCCESS:
                handleNotification(msg, oos);
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
            PeerInfo newPeer = new PeerInfo(username, password);

            // Insert new registrant peer to HashMap
            peers.put(username, newPeer);
            oos.writeObject(new Message(MessageType.RESPONSE, "Registered successfully"));
        }
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

            // Insert peer to the connectedPeers hashMap
            connectedPeers.put(token, peer);

            // Gather IP address and port information
            String ipAddress = clientSocket.getInetAddress().getHostAddress();
            System.out.println(ipAddress);
            int port = clientSocket.getPort();

            // Inform peer of the successful login and send the session token
            Message reply = new Message(MessageType.LOGIN_SUCCESS, "Login successful: Token=" + token);
            reply.setToken(token);
            reply.setContent("IP Address: " + ipAddress + ", Port: " + port + ", Token: " + token);
            oos.writeObject(reply);

        } else {
            // Send an error message if the login fails
            oos.writeObject(new Message(MessageType.ERROR, "Login failed"));
        }
    }

    private void informationFromPeer(Message msg, ObjectOutputStream oos) throws IOException{

        PeerInfo peer = connectedPeers.get(msg.getToken());

        // Check if Message intends to inform Tracker about Peer's IP and Port
        if (msg.getContent() != null) {
            String[] msgData = msg.getContent().split(",");
            // not best practice with split
            peer.setIp(msgData[0]);
            peer.setPort(Integer.parseInt(msgData[1]));
        }

        // If Message contains HashMap FileDetails
        // Setting the initialSeederBit for each file that this peer is initial seeder
        if (!msg.getFileDetails().isEmpty()){

            // Set whole file names
            peer.setFiles(msg.getFiles());

            // Set fragments for each file
            peer.setFragments(msg.getFragments());

            // Inform the ListFileDownload.txt
            saveToFileDownloadList(msg.getFiles());

            for (String fileName: msg.getFileDetails().keySet()){
                peer.getIsFileInitSeeder().put(fileName, true);
            }
        }

    }

    private void saveToFileDownloadList(ArrayList<String> files) throws IOException{
        String dir = Paths.get(System.getProperty("user.dir")).resolve("src").toString()+"\\ListfileDownload.txt";
        BufferedReader br = new BufferedReader(new FileReader(dir));
        BufferedWriter writer = new BufferedWriter(new FileWriter(dir));
        for (String file: files){
            String line;
            Boolean flag = false;
            while((line = br.readLine()) != null){
                if (line.equals(file)){
                    flag = true;
                    break;
                }
            }
            if (!flag){
                writer.append(file).append("\n");
                writer.flush();
            }
        }
    }

    private void performLogout(Message msg, ObjectOutputStream oos) throws IOException {
        String token = msg.getToken();

        // If client with current token is connected
        if (connectedPeers.containsKey(token)) {
            PeerInfo peer = connectedPeers.get(token);
            connectedPeers.remove(token);
            peer.setToken(null);// Invalidate the token
            peer.setIp(null);
            peer.setPort(0);
            msg = new Message(MessageType.LOGOUT_SUCCESS, "Successful logout");
            oos.writeObject(msg);

            connectedPeers.remove(token);
            //clientConnected = false;
        } else {
            oos.writeObject(new Message(MessageType.INFORM));

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

    /**
     * Handles the incoming requests from peers, including listing files.
     */
    private void listFiles(ObjectOutputStream oos) throws IOException {
        HashSet<String> allFiles = new HashSet<>();
        for (PeerInfo peerInfo : peers.values()) {
            if (peerInfo.getFiles() != null) {
                allFiles.addAll(peerInfo.getFiles());
            }
        }
        // Check if there are any files to send
        if (!allFiles.isEmpty()) {
            oos.writeObject(new Message(MessageType.RESPONSE, new ArrayList<>(allFiles)));
        } else {
            oos.writeObject(new Message(MessageType.RESPONSE, "No files available"));
        }
    }

    private void handleDetails(Message msg, ObjectOutputStream oos) throws IOException {

        // Name of the file requested
        String requestedFile = msg.getContent();
        ArrayList<PeerInfo> peersWithFile = new ArrayList<>();



        // Iterate over the collection of connectedPeers and add those with the requested file to the list
        for (PeerInfo peer : connectedPeers.values()) {
            if (peer.getFiles().contains(requestedFile)) {

                // Checks if this peer is active before inserting the object in the peersWithFile
                if (checkActive(peer)) {

                    peersWithFile.add(peer);
                }
            }
        }

        // Find the total fragments of the file from the Initial seeder
        HashMap<String, ArrayList<String>> totalFragments = new HashMap<>();
        for (PeerInfo peer : peers.values()) {
            if (peer.getFiles().contains(requestedFile)) {

                totalFragments.put(requestedFile, peer.getFragments().get(requestedFile));

            }
        }

        if (peersWithFile.isEmpty()) {
            oos.writeObject(new Message(MessageType.ERROR, "No peers have the file"));
        } else {
            // Respond with the peers that have the file requested
            Message response = new Message(MessageType.RESPONSE, requestedFile);
            response.setFragments(totalFragments);
            response.setPeers(peersWithFile);
            System.out.println(peersWithFile.getLast().getIsFileInitSeeder().get(requestedFile));
            oos.writeObject(response);
        }
    }

    public Boolean checkActive(PeerInfo peer){

        try {
            Socket socket = new Socket(peer.getIp(), peer.getPort());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new Message(MessageType.CHECK_ACTIVE));
            Object msg = in.readObject();

            // If Peer responds with MessageType.ACTIVE_RESPONSE then return true
            if (msg instanceof Message && ((Message) msg).getType() == MessageType.ACTIVE_RESPONSE){
                return true;
            }
        }
        catch (IOException  e){
            System.out.println("Peer is not responding");
            return false;
        }
        catch (ClassNotFoundException e){
            e.printStackTrace();
        }

        return null;
    }


    public void handleNotification(Message msg, ObjectOutputStream oos){

        // If download was successful update seeder's values
        if (msg.getType() == MessageType.NOTIFY_SUCCESS){

            // Update countDownloads of seeder
            peers.get(msg.getUsername()).incCountDownloads();

            // Update sharedFiles of the user that downloaded the file
            connectedPeers.get(msg.getToken()).getFiles().add(msg.getContent());
        }
        else {

            // Increment the failures counter of the seeder peer.
            peers.get(msg.getUsername()).incCountfailures();
        }
    }

    /*
    public void completeDownload(boolean success, String fileName) {
        Message downloadResult = new Message(success ? MessageType.DOWNLOAD_SUCCESS : MessageType.DOWNLOAD_FAIL, fileName);
        downloadResult.setContent("Download " + (success ? "successful" : "failed") + " for " + fileName);
        try {
            oos.writeObject(downloadResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}
