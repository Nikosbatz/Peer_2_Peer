package Peers;

import Tracker.PeerInfo;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Peer {
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String token;
    private int port;
    private String ip;

    public static Boolean processRunning;

    public Peer(String trackerHost, int trackerPort, int port) throws IOException {
        socket = new Socket(trackerHost, trackerPort);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
        this.port = port;
    }
    private ArrayList<String> getSharedDirectoryInfo() {
        // Path for this peer's shared_directory
        Path currentDir = Paths.get(System.getProperty("user.dir")).resolve("src");


        String shared_dir = currentDir.resolve("shared_Directory1").toString();
        System.out.println(shared_dir);
        File dir = new File(shared_dir);

        // if directory is not empty
        if (dir.listFiles() != null){
            ArrayList<File> files = new ArrayList<>(Arrays.asList(dir.listFiles()));
            ArrayList<String> fileNames = new ArrayList<>();
            for (File file: files){
                fileNames.add(file.getName());
            }
            return fileNames;
        }
        else{
            return null;
        }

    }





    // ---------User authentication methods---------
    public void registerWithTracker(String username, String password) throws IOException, ClassNotFoundException {
        Message msg = new Message(MessageType.REGISTER);


        //TODO Ask user to input USERNAME and PASSWORD for the registration

        msg.setContent(username+":"+password);

        oos.writeObject(msg);
        Object response = ois.readObject();
        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            System.out.println("Registration Response: " + responseMessage.getContent());
        }
    }

    public void login(String username, String password) throws IOException, ClassNotFoundException {
        // Create the login message with username and password
        Message loginMessage = new Message(MessageType.LOGIN, username + ":" + password);
        oos.writeObject(loginMessage); // Send login message to the tracker

        // Await the response from the tracker
        Object response = ois.readObject();

        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            if (responseMessage.getType() == MessageType.LOGIN_SUCCESS) {
                // Extract token
                String[] details = responseMessage.getContent().split(",");
                this.token = details[2].split(":")[1].trim();  //  "IP Address: x.x.x.x, Port: xxxx, Token: xxx"
                String ipAddress = details[0].split(":")[1].trim();
                int port = Integer.parseInt(details[1].split(":")[1].trim());

                System.out.println("Login successful. Token: " + this.token);
                System.out.println("Assigned IP: " + ipAddress);
                System.out.println("Assigned Port: " + port);

                //  send additional information back to the tracker (INFORM METHOD)
                sendAdditionalInfo();
            } else {
                System.out.println("Login failed. Reason: " + responseMessage.getContent());
            }
        } else {
            System.out.println("Unexpected response type from server.");
        }
    }
    public void logOut() throws IOException, ClassNotFoundException{
        Message msg = new Message(MessageType.LOGOUT);
        msg.setToken(this.token);
        oos.writeObject(msg);

        // Wait for server reply
        Object reply = ois.readObject();

        if (reply instanceof Message){

            Message rep = (Message) reply;
            System.out.println(rep.getContent());

            // If Tracker replies with successful logout then token = null
            if (((Message) reply).getType() == MessageType.LOGOUT_SUCCESS){
                this.token = null;
            }
            else{
                System.out.println("DEN KANEI LOGOUT");
            }
        }
    }
    //login help function
    private void sendAdditionalInfo() throws IOException {

        String ip = getIp();
        int port = getPort();

        Message additionalInfoMessage = new Message(MessageType.INFORM, ip + "," + port );
        additionalInfoMessage.setToken(this.token);
        additionalInfoMessage.setFiles(getSharedDirectoryInfo());
        oos.writeObject(additionalInfoMessage);
    }

//-----------File managment methods----------
    /**
     * Requests the list of all files available in the P2P network from the tracker.
     * Prints out the list or indicates if no files are available.
     */
    public void listFiles() throws IOException, ClassNotFoundException {
        // Send a LIST_FILES message to the tracker
        Message listRequest = new Message(MessageType.LIST_FILES);
        oos.writeObject(listRequest);

        // Wait for the response from the tracker
        Object response = ois.readObject();
        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            if (!responseMessage.getContent().isEmpty()) {
                System.out.println("Available files: " + responseMessage.getContent());
            } else {
                System.out.println("No files available in the system.");
            }
        } else {
            System.out.println("Received an invalid response from the tracker.");
        }
    }

    // Method to check if a specific peer is active by sending a token or identifier
    public long checkActive(PeerInfo peer) throws IOException, ClassNotFoundException {
        long startTime = System.currentTimeMillis();

        // Open new Socket with the peer that owns the file
        try {
            Socket socket = new Socket(peer.getIp(), peer.getPort());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new Message(MessageType.CHECK_ACTIVE));
            Object msg = in.readObject();
            long responseTime = System.currentTimeMillis() - startTime;
            // If Peer responds with MessageType.ACTIVE_RESPONSE then return true
            if (msg instanceof Message && ((Message) msg).getType() == MessageType.ACTIVE_RESPONSE){
                return responseTime;
            }
        }
        catch (IOException  e){
            System.out.println("Peer is not responding");
            return (Long.MAX_VALUE) ;
        }
        catch (ClassNotFoundException e){
            e.printStackTrace();
        }

        // Wait for the response from the tracker or peer
        return  Long.MAX_VALUE;
    }
  //File details
    public void requestFileDetails(String fileName) throws IOException, ClassNotFoundException {
        Message requestDetails = new Message(MessageType.DETAILS, fileName);
        oos.writeObject(requestDetails);


        Object response = ois.readObject();

        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            System.out.println(responseMessage.getType());
            handleFileDetailsResponse(responseMessage);
        }
    }

    private void handleFileDetailsResponse(Message responseMessage) throws IOException, ClassNotFoundException {
        ArrayList <PeerInfo> peersWithFile = responseMessage.getPeers();

        double bestScore = 200000000;
        PeerInfo bestPeer = null;


        // Calculates each peer's score based on Downloads and Failures
        // Choosing the one with the best ratio after
        for (PeerInfo peer : peersWithFile) {

            int downloads = peer.getCountDownloads();
            int failures = peer.getCountFailures();
            double score = calculateScore(downloads, failures)+checkActive(peer);

            if (score < bestScore) {
                bestScore = score;
                bestPeer = peer;
            }
        }


        if (bestPeer != null) {
            Scanner in = new Scanner(System.in);
            System.out.println("Best peer details:\nUsername: "+bestPeer.getUsername()+"\nIP: "+bestPeer.getIp()+"\nPort: "+bestPeer.getPort());
            System.out.println("Do you want to download the file? y/n");
            String response = in.nextLine();

            // If user wishes to download the file from this peer
            if(response.equals("y")){
                initiateDownloadFromPeer(bestPeer, responseMessage.getContent());
            }

        } else {
            System.out.println("No suitable peers found for downloading.");
        }
    }

    private double calculateScore(int downloads, int failures) {
        return Math.pow(0.75, downloads) * Math.pow(1.25, failures);//0.75^count_downloads*1.25^count_failures.
    }

//Downloading
    private void initiateDownloadFromPeer(PeerInfo bestPeer , String fileName) {
        //TODO bestPeerId is PeerInfo object
        try {
            // Extract peer IP and port

            String peerIP = bestPeer.getIp();
            System.out.println(peerIP);
            int peerPort = bestPeer.getPort();

            try (Socket peerSocket = new Socket(peerIP, peerPort);
                 ObjectOutputStream peerOut = new ObjectOutputStream(peerSocket.getOutputStream());
                 ObjectInputStream peerIn = new ObjectInputStream(peerSocket.getInputStream())) {

                peerOut.writeObject(new Message(MessageType.DOWNLOAD_REQUEST, fileName));
                Message fileResponse = (Message) peerIn.readObject();

                if (fileResponse.getType() == MessageType.FILE_RESPONSE && fileResponse.getFileContent() != null) {
                    Path downloadPath = Paths.get(System.getProperty("user.dir"), "downloads");
                    Files.createDirectories(downloadPath);
                    downloadPath = downloadPath.resolve(fileName);
                    Files.write(downloadPath, fileResponse.getFileContent());
                    System.out.println("File downloaded successfully to " + downloadPath.toString());
                    notifyTrackerFileAvailable(fileName, bestPeer);
                } else {
                    System.out.println("Failed to download the file: " + fileResponse.getContent());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error during file download: " + e.getMessage());
        }
    }


    // This method notifies the tracker that the file is now available from this peer
    private void notifyTrackerFileAvailable(String fileName, PeerInfo providerPeer) throws IOException {
        Message notifyMsg = new Message(MessageType.NOTIFY, fileName);
        notifyMsg.setContent("Download successful from " + providerPeer.getUsername());
        oos.writeObject(notifyMsg);
    }


    public void showMenu () throws IOException, ClassNotFoundException{
        Scanner in = new Scanner(System.in);

        // If peer in not connected show the login/register forms
        if (this.token == null) {
            String username;
            String password;
            System.out.print("Choose an option:\n1. Registration\n2. Login\n3. Exit\nEnter your choice: ");
            String choice = in.nextLine();
            switch (choice){
                case "1":
                    System.out.print("Username: ");
                    username = in.nextLine();
                    System.out.print("\nPassword: ");
                    password = in.nextLine();
                    registerWithTracker(username, password);
                    break;
                case "2":
                    System.out.print("Username: ");
                    username = in.nextLine();
                    System.out.print("\nPassword: ");
                    password = in.nextLine();
                    login(username, password);
                    break;
                case "3":
                    processRunning = false;
                    break;
            }
        }
        // If peer is connected show the operations menu
        else {
            //TODO make the operations menu
            System.out.print("Choose an option:\n1.Logout\n2.List available files \n3.Give details about a file \n4.Download a file\n5.Exit \nEnter your choice: ");
            String choice = in.nextLine();
            switch (choice){
                case "1":
                    logOut();
                    break;
                case "2":
                    listFiles();
                    break;
                case"3":
                    System.out.println("Enter the filename for details\n");
                    String details_file=in.nextLine();
                    requestFileDetails(details_file);
                    break;
                case "4":
                    System.out.println("Enter the filename to download");
                    String download_file=in.nextLine();
                    requestFileDetails(download_file);
                    break;
                case"5":
                    System.out.println("exiting...");
                    break;
            }

        }
    }


    public static void main(String[] args) {
        try {
            Peer peer = new Peer("localhost", 1111, 1112);

            // Start a PeerServer where Peer accepts requests from other Peers or the Tracker.
            new Thread(new peerServer()).start();

            // Defines the Files that Peer wants to share
            peer.getSharedDirectoryInfo();

            // Process runs until 'Exit' option from menu is selected.
            processRunning = true;
            while(processRunning) {
                peer.showMenu();
            }


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        return "localhost";
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
