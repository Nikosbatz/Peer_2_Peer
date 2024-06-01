package Peers;

import Tracker.PeerInfo;

import java.awt.image.AreaAveragingScaleFilter;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;


public class Peer {
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String username;
    private String token;
    private Thread serverThread;
    private ServerSocket serverSocket;

    private Boolean isFirstLogin = true;

    private int port = 1115;

    private final String shared_dir = "shared_Directory5";
    public static Boolean processRunning;
    //the outer part uses the filename that is being downloaded as the key ,the inner one uses the part number as the key
    // and the username of the peer who sent that part as the value,this is used to keep track of the parts of the file
    //and the peer from whom they were received

    // (Key = username, value = file parts)
    public ConcurrentHashMap<String, ArrayList<String>> filePartsReceivedFrom;

    public Peer(String trackerHost, int trackerPort) throws IOException {
        socket = new Socket(trackerHost, trackerPort);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
        this.filePartsReceivedFrom = new ConcurrentHashMap<>();

    }

    public ObjectInputStream getOis(){
        return ois;
    }
    public ObjectOutputStream getOos(){
        return  oos;
    }
    //Method to upadte the filePartsRecievedFrom


    //TODO use it in the download section



    ArrayList<String> getSharedDirectoryInfo() {
        // Path for this peer's shared_directory
        Path currentDir = Paths.get(System.getProperty("user.dir")).resolve("src");
        String shared_dir = currentDir.resolve(this.shared_dir).toString();

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
                this.username = username;
                // Extract token
                String[] details = responseMessage.getContent().split(",");
                this.token = details[2].split(":")[1].trim();  //  "IP Address: x.x.x.x, Port: xxxx, Token: xxx"
                String ipAddress = details[0].split(":")[1].trim();
                int port = Integer.parseInt(details[1].split(":")[1].trim());

                System.out.println("Login successful. Token: " + this.token);
                System.out.println("Assigned IP: " + ipAddress);
                System.out.println("Assigned Port: " + port);

                // Start the Peer's Server to start exchanging files with other Peers
                startPeerServer();

                // If this is the first LogIn of this Peer inform
                // Tracker of the files in possession
                if (isFirstLogin) {
                    // Check if the peer is the initial seeder for any files
                    ArrayList<String> files = getSharedDirectoryInfo();
                    if (files != null && !files.isEmpty()) {
                        notifyTrackerSeederStatus();
                    }
                }
                // Else just Login ( Tracker is already aware of the files that the Peer owns )
                else {
                    isFirstLogin = false;
                }
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
                System.out.println("Unsuccessful logout...");
            }
        }
    }

//-----------File managment methods----------
    /**
     * Requests the list of all files available in the P2P network from the tracker.
     * Prints out the list or indicates if no files are available.
     */
    public ArrayList<String> listFiles() throws IOException, ClassNotFoundException {
        // Send a LIST_FILES message to the tracker
        Message listRequest = new Message(MessageType.LIST_FILES);
        oos.writeObject(listRequest);

        // Wait for the response from the tracker
        Object response = ois.readObject();
        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            if (!responseMessage.getFiles().isEmpty()) {
                return responseMessage.getFiles();
                //System.out.println("Available files: " + responseMessage.getFiles());
            }
        } else {
            System.out.println("Received an invalid response from the tracker.");
        }
        return null;
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
    public ArrayList<PeerInfo> requestFileDetails(String fileName) throws IOException, ClassNotFoundException {
        Message requestDetails = new Message(MessageType.DETAILS, fileName);
        oos.writeObject(requestDetails);


        Object response = ois.readObject();

        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            responseMessage.setContent(fileName);

            return handleFileDetailsResponse(responseMessage, fileName);
        }
        else {
            return null;
        }

    }

    private ArrayList<PeerInfo> handleFileDetailsResponse(Message responseMessage, String fileName) throws IOException, ClassNotFoundException {

        ArrayList<PeerInfo> peersWithFile = responseMessage.getPeers();
        if (peersWithFile == null) {
            System.out.println("There aren't any connected Peers that can share this file...");
        }
        else {
            HashMap<PeerInfo, Double> peerScores = new HashMap<>();
            double bestScore = 200000000;
            PeerInfo bestPeer = null;


            // Calculates each peer's score based on Downloads and Failures
            // Choosing the one with the best ratio after
            for (PeerInfo peer : peersWithFile) {

                int downloads = peer.getCountDownloads();
                int failures = peer.getCountFailures();
                double score = calculateScore(downloads, failures) + checkActive(peer);

                if (score < bestScore) {
                    bestScore = score;
                    bestPeer = peer;
                }
                peerScores.put(peer, score);
            }

            // Prints the info of every peer that has the file

            for(PeerInfo peer: peersWithFile){
                System.out.println("Username: " + peer.getUsername() + "\tIP: " + peer.getIp() + "\tPort: " + peer.getPort() +
                        "\nCount Failures: " + peer.getCountFailures() +"\nCount Downloads: "+ peer.getCountDownloads()+
                        "\nFragments: " + peer.getFragments().get(fileName).toString() + "\nSeeder-bit: " + peer.getIsFileInitSeeder().get(fileName));
            }

           /* Scanner in = new Scanner(System.in);
            System.out.println("Best peer details:\nUsername: " + bestPeer.getUsername() + "\nIP: " + bestPeer.getIp() + "\nPort: " + bestPeer.getPort());
            System.out.println("Do you want to download the file? y/n");
            String response = in.nextLine();

            // If user wishes to download the file from this peer.
            // Long.MAX_VALUE is the value checkActive returns if the peer is not active.
            if (response.equals("y") ) {
                ArrayList<PeerInfo> failedPeers = new ArrayList<>();
                initiateDownloadFromPeer(bestPeer, responseMessage, peerScores, failedPeers);
            }*/

        }
        return peersWithFile;
    }

    private double calculateScore(int downloads, int failures) {
        return Math.pow(0.75, downloads) * Math.pow(1.25, failures);//0.75^count_downloads*1.25^count_failures.
    }

    //--------------------File partitioning-----------------
    // Method to partition a file into segments
    public void partitionFile(String fileName) throws IOException {

        // Path for this peer's shared_directory
        Path currentDir = Paths.get(System.getProperty("user.dir")).resolve("src");
        String filePath = currentDir.resolve(this.shared_dir).toString() + File.separator + fileName;
        File file = new File(filePath);

        // Init partitioning buffer size
        int partSize = 1024 * 1024; // 1MB per part
        byte[] buffer = new byte[partSize];

        // Get the name of the file without the extension (.txt)
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            fileName = fileName.substring(0, dotIndex);
        }

        // Read the file that is about to be Partitioned
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            int bytesAmount;
            int partCounter = 1;
            // For each 1MB create a new file
            while ((bytesAmount = bis.read(buffer)) > 0) {
                String filePartName = String.format("%s.part_%d.txt", fileName, partCounter++);
                File newFile = new File("src\\"+shared_dir, filePartName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
            }
        }
    }

    // Initialize and partition files when the peer is set as a seeder
    /*public void initializeSeeder() throws IOException {
        // Path for this peer's shared_directory
        Path currentDir = Paths.get(System.getProperty("user.dir")).resolve("src");
        String directoryPath = currentDir.resolve(this.shared_dir).toString();

        File dir = new File(directoryPath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                partitionFile(child);
            }
        }
    }*/

    //seeder inform 1
    //if the peer is the initial seeder for the file
    //1.when the seeder receives the token id from a successful login he updates
    //the tracker with the communication details(ip,port) for the current contents of the shared directory and
    // that he can work as a seeder for them
    private void notifyTrackerSeederStatus() throws IOException {

        // Construct the message to inform the tracker
        String ip = getIp();
        int port = getPort();
        Message seederInfoMessage = new Message(MessageType.INFORM, ip + "," + port );
        seederInfoMessage.setToken(this.token);

        // Get the shared directory info
        ArrayList<String> files = getSharedDirectoryInfo();

        //HashMap <name of file, list of its fragments>
        HashMap<String, ArrayList<String>> fragments = new HashMap<>();
        ArrayList<String> tempFragments;

        // Partition each file and organize its fragments in the fragments HashMap
        for (String file: files){
            if (!file.contains("part_")){
                partitionFile(file);
                // the peer has all pieces of the file, indicating seeder status
                seederInfoMessage.addFileDetail(file, true);
                tempFragments = new ArrayList<>();

                // Iterate through the shared_dir files
                for (String tempFile: getSharedDirectoryInfo()){
                    // If tempFile is part of the current file add it to the fragments
                    if (tempFile.contains("part_") && tempFile.contains(file.substring(0, file.lastIndexOf('.')))){
                        tempFragments.add(tempFile);
                    }
                }
                // Insert the name of file and the fragments that assemble it
                fragments.put(file, tempFragments);
            }
        }
        // Set Message parameters
        seederInfoMessage.setFiles(files);
        seederInfoMessage.setFragments(fragments);

        // Send the message to the tracker
        oos.writeObject(seederInfoMessage);
    }
    private void startPeerServer() throws IOException {
        this.serverSocket = new ServerSocket(getPort());
        this.serverThread = new Thread(new peerServer(this.serverSocket, this.shared_dir, this));
        serverThread.start();
    }
    //Select functionality
    public void select() throws IOException, ClassNotFoundException {
        // Get the list of files available in the network
        Message listRequest = new Message(MessageType.LIST_FILES);
        oos.writeObject(listRequest);

        // Wait for the response from the tracker
        Object response = ois.readObject();
        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            if (!responseMessage.getContent().isEmpty()) {
                ArrayList<String> availableFiles = new ArrayList<>(Arrays.asList(responseMessage.getContent().split(",")));

                // Get the list of files the peer already has
                ArrayList<String> localFiles = getLocalFiles();

                // Filter out the files the peer already has
                availableFiles.removeAll(localFiles);

                if (!availableFiles.isEmpty()) {
                    // Randomly select a file from the remaining list
                    Random random = new Random();
                    String selectedFile = availableFiles.get(random.nextInt(availableFiles.size()));

                    System.out.println("Selected file for download: " + selectedFile);

                    // Initiate the download for the selected file
                    requestFileDetails(selectedFile);
                } else {
                    System.out.println("All available files are already downloaded.");
                }
            } else {
                System.out.println("No files available in the system.");
            }
        } else {
            System.out.println("Received an invalid response from the tracker.");
        }
    }

    private ArrayList<String> getLocalFiles() {
        Path currentDir = Paths.get(System.getProperty("user.dir")).resolve("src").resolve(shared_dir);
        File dir = new File(currentDir.toString());
        if (dir.listFiles() != null) {
            ArrayList<File> files = new ArrayList<>(Arrays.asList(dir.listFiles()));
            ArrayList<String> fileNames = new ArrayList<>();
            for (File file : files) {
                fileNames.add(file.getName());
            }
            return fileNames;
        } else {
            return new ArrayList<>();
        }
    }
    private HashMap<String, String> getPeerInfoFromRequest(RequestInfo requestInfo) throws IOException, ClassNotFoundException {
        Message peerInfoRequest = new Message(MessageType.PEER_INFO, requestInfo.peerUsername);
        oos.writeObject(peerInfoRequest);

        Message response = (Message) ois.readObject();
        return response.getPeerInfoDetails(); // Ensure this returns the necessary peer info details as a HashMap
    }


    //TODO------------------------------------
    private void downloadFiles() throws IOException, ClassNotFoundException, InterruptedException {
        Random random = new Random();
        ArrayList<String> files = listFiles();
        System.out.println("Files size: " + files.size());

        while(!files.isEmpty()) {

            System.out.println("-------------------");

            // Randomly select a file
            String selectedFile = files.get(random.nextInt(files.size()));

            // Request The total fragments number of this file from the Tracker
            Message msg = new Message(MessageType.TOTAL_FRAGMENTS, selectedFile);
            oos.writeObject(msg);


            // Wait for the reply
            Message response = (Message) ois.readObject();

            int totalFileFragments = response.getTotalFileFragments();

            ArrayList<String> localFiles = getSharedDirectoryInfo();

            // if Peer already has this file
            // continue to download another file
            if (localFiles.contains(selectedFile)){
                files.remove(selectedFile);
                continue;
            }

            System.out.println(selectedFile);
            // Remove the selected file after the random choice
            files.remove(selectedFile);


            // Send the request to the tracker
            Message requestDetails;
            synchronized (oos) {
                requestDetails = new Message(MessageType.DETAILS, selectedFile);
                oos.writeObject(requestDetails);

                // Get the response of the file details
                response = (Message) ois.readObject();
            }

            System.out.println(response.getPeers() + "," + response.getFragments() + "," + response.getType());
            ArrayList<PeerInfo> peersWithFile = response.getPeers();
            ArrayList<String> fileFragments = response.getFragments().get(selectedFile);
            ArrayList<MessageType> downloadResults = new ArrayList<>();



            ArrayList<String> missingFragments = getMissingFragments(fileFragments);

            while (!missingFragments.isEmpty()) {

                //TODO implement requests TIMER
                long startTime = System.currentTimeMillis();

                if (peersWithFile.size() <= 4) {
                    for (PeerInfo peer : peersWithFile) {

                        // Start new Thread to Request file fragments from current peer
                        new Thread(new DownloadRequestHandler(peer, selectedFile, this, downloadResults, response)).start();

                    }
                } else {
                    HashMap<PeerInfo, Integer> peerFragmentCount = new HashMap<>();
                    for (PeerInfo peer : peersWithFile) {
                        int fragmentCount = getFragmentCount(peer, selectedFile);
                        peerFragmentCount.put(peer, fragmentCount);
                    }

                    ArrayList<PeerInfo> selectedPeers = selectPeers(peerFragmentCount, 4, selectedFile);
                    for (PeerInfo peer : selectedPeers) {
                        new Thread(new DownloadRequestHandler(peer, selectedFile, this, downloadResults, response)).start();
                    }
                }

                // Timer
                while(System.currentTimeMillis() - startTime < 500){}

                missingFragments = getMissingFragments(fileFragments);

                // Reload peersWithFile
                synchronized (oos) {
                    requestDetails = new Message(MessageType.DETAILS, selectedFile);
                    oos.writeObject(requestDetails);

                    response = (Message) ois.readObject();
                    peersWithFile = response.getPeers();
                    // -------- Reload end ----------
                }
            }


            //assembleFile(selectedFile);
            msg = new Message(MessageType.NOTIFY_SUCCESS, selectedFile);
            msg.setToken(this.token);
            oos.writeObject(msg);
            oos.flush();

        }

    }

    private ArrayList<String> getMissingFragments (ArrayList<String> fileFragments) throws IOException, ClassNotFoundException {

        ArrayList<String> localFiles = getSharedDirectoryInfo();
        ArrayList<String> missingFragments = new ArrayList<>();

        for (String fragment: fileFragments){
            if (!localFiles.contains(fragment)){
                missingFragments.add(fragment);
            }
        }
        return missingFragments;

    }

    public void updateFilePartsReceivedFrom(String fileName, String peerUsername){

        if (!filePartsReceivedFrom.containsKey(peerUsername)){
            filePartsReceivedFrom.put(peerUsername, new ArrayList<>());
        }
        filePartsReceivedFrom.get(peerUsername).add(fileName);
    }

    private void assembleFile(String fileName){
        //TODO assemble the downloaded file
    }


    private int getFragmentCount(PeerInfo peer, String fileName) {
        if (peer.getFragments().containsKey(fileName)) {
            return peer.getFragments().get(fileName).size();
        }
        return 0;
    }
    private ArrayList<PeerInfo> selectPeers(HashMap<PeerInfo, Integer> peerFragmentCount, int count, String selectedfile) {
        ArrayList<PeerInfo> selectedPeers = new ArrayList<>();
        ArrayList<PeerInfo> peers = new ArrayList<>(peerFragmentCount.keySet());

        // Select 2 peers with the most fragments
        peers.sort((p1, p2) -> peerFragmentCount.get(p2) - peerFragmentCount.get(p1));

        for (int i = 0; i < 2 && i < peers.size(); i++) {
            if(!peers.get(i).getIsFileInitSeeder().get(selectedfile)) {
                selectedPeers.add(peers.get(i));
            }
        }

        // Select 2 random peers
        peers.removeAll(selectedPeers);
        Random random = new Random();
        for (int i = 0; i < 2 && !peers.isEmpty(); i++) {
            selectedPeers.add(peers.remove(random.nextInt(peers.size())));
        }

        return selectedPeers;
    }


    //-----------------Downloading-------------------------
    private void initiateDownloadFromPeer(PeerInfo bestPeer, Message responseMsg, HashMap<PeerInfo, Double> scores, ArrayList<PeerInfo> failedPeers) {
        String fileName = responseMsg.getContent();

        try {
            // Extract peer IP and port
            System.out.println(bestPeer.getIp() + "     "+bestPeer.getPort());

            try (Socket peerSocket = new Socket(bestPeer.getIp(), bestPeer.getPort());
                 ObjectOutputStream peerOut = new ObjectOutputStream(peerSocket.getOutputStream());
                 ObjectInputStream peerIn = new ObjectInputStream(peerSocket.getInputStream())) {

                // Construct download request based on the missing fragments of the file
                Message msg = new Message(MessageType.DOWNLOAD_REQUEST, fileName);
                HashMap<String, ArrayList<String>> fragments = new HashMap<>();
                fragments.put(fileName,getMissingFragments(responseMsg, bestPeer));
                msg.setFragments(fragments);

                // Send Download request to Peer
                peerOut.writeObject(msg);
                System.out.println("TO ESTEILEEE");



                Message fileResponse = (Message) peerIn.readObject();
                System.out.println("TO ESTEILEEE");

                if (fileResponse.getType() == MessageType.FILE_RESPONSE && fileResponse.getFileContent() != null) {
                    Path downloadPath = Paths.get(System.getProperty("user.dir"), "src\\"+this.shared_dir);
                    Files.createDirectories(downloadPath);
                    // fileResponse.getContent contains the name of the file that the seeder sent
                    downloadPath = downloadPath.resolve(fileResponse.getContent());
                    Files.write(downloadPath, fileResponse.getFileContent());
                    System.out.println("File downloaded successfully to " + downloadPath.toString());
                    notifyTrackerSuccess(fileName, bestPeer);
                } else {
                    notifyTrackerFail(bestPeer);
                    System.out.println("Failed to download the file: " + fileResponse.getContent());
                    retryDownload(responseMsg, bestPeer, scores, failedPeers);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Error during file download: " + e.getMessage());
            notifyTrackerFail(bestPeer);
            retryDownload(responseMsg, bestPeer, scores, failedPeers);
        }
    }

    private void retryDownload(Message responseMsg, PeerInfo failedPeer, HashMap<PeerInfo, Double> scores, ArrayList<PeerInfo> failedPeers) {
        String filename = responseMsg.getContent();
        failedPeers.add(failedPeer);
        PeerInfo nextBestPeer = getNextBestPeer(filename, failedPeer, scores, failedPeers);
        if (nextBestPeer != null) {
            System.out.println("Attemting to download from another peer");
            initiateDownloadFromPeer(nextBestPeer, responseMsg, scores, failedPeers);

        } else {
            System.out.println("No other active peers have this file.\n Download terminated....");
        }
    }

    private PeerInfo getNextBestPeer(String filename, PeerInfo failedPeer, HashMap<PeerInfo, Double> scores, ArrayList<PeerInfo> failedPeers) {
        Map.Entry<PeerInfo, Double> maxEntry = null;
        for (Map.Entry<PeerInfo, Double> entry : scores.entrySet()) {
            if ((maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) && !failedPeers.contains(entry.getKey())) {
                maxEntry = entry;
            }
        }
        if (maxEntry == null){
            return null;
        }
        else {
            return maxEntry.getKey();
        }
    }

    // Notify Tracker if requested File downloaded succesfully.
    private void notifyTrackerSuccess(String fileName, PeerInfo peer) throws IOException {
        Message notifyMsg = new Message(MessageType.NOTIFY_SUCCESS, fileName);
        notifyMsg.setToken(this.token);
        notifyMsg.setUsername(peer.getUsername());
        notifyMsg.setContent(fileName);
        oos.writeObject(notifyMsg);

    }


    private void notifyTrackerFail(PeerInfo peer){
        Message notifyMsg = new Message(MessageType.NOTIFY_FAIL);
        notifyMsg.setUsername(peer.getUsername());
    }

    private ArrayList<String> getMissingFragments(Message responseMsg, PeerInfo bestPeer){

        String fileName = responseMsg.getContent();
        ArrayList<String> fragments = bestPeer.getFragments().get(fileName);
        ArrayList<String> files = getSharedDirectoryInfo();
        ArrayList<String> missingFragments = new ArrayList<>();


        for (String fragment: fragments){
            if (!files.contains(fragment)){
                missingFragments.add(fragment);
            }
        }

        return missingFragments;
    }



    public void showMenu () throws IOException, ClassNotFoundException, InterruptedException {
        Scanner in = new Scanner(System.in);

        // If peer in not connected show the login/register forms
        if (this.token == null) {
            String username;
            String password;
            System.out.print("Choose an option:\n1. Registration\n2. Login\n3. Exit\nEnter your choice: ");
            String choice = in.nextLine();
            switch (choice) {
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
                    if (this.token != null){
                        this.serverThread = new Thread(new peerServer(this.serverSocket, this.shared_dir, this));
                        serverThread.start();
                    }

                    break;
                case "3":
                    processRunning = false;
                    break;
            }
        }
        // If peer is connected show the operations menu
        else {
            //TODO make the operations menu
            System.out.print("Choose an option:\n1.Logout\n2.List available files \n3.Give details about a file \n4.Download a file\n5.Download all files\n6.Exit \nEnter your choice: ");
            String choice = in.nextLine();
            switch (choice) {
                case "1":
                    logOut();
                    this.serverSocket.close();
                    try {
                        System.out.println("Waiting for thread to join...");
                        this.serverThread.join();
                    }
                    catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    break;
                case "2":
                    listFiles();
                    break;
                case "3":
                    System.out.println("Enter the filename for details\n");
                    String details_file = in.nextLine();
                    requestFileDetails(details_file);
                    break;
                case "4":
                    System.out.println("Enter the filename to download");
                    String download_file = in.nextLine();
                    requestFileDetails(download_file);
                    break;

                case "5":
                    System.out.println("Download all files of P2P");
                    downloadFiles();
                    break;
                case "6":
                    System.out.println("exiting...");
                    break;
            }

        }


    }
    public static void main(String[] args) {
        try {
            Peer peer = new Peer("localhost", 1111);

            // Start a PeerServer where Peer accepts requests from other Peers or the Tracker.


            // Defines the Files that Peer wants to share
            peer.getSharedDirectoryInfo();

            // Process runs until 'Exit' option from menu is selected.
            processRunning = true;
            while(processRunning) {
                peer.showMenu();
            }


        } catch (IOException | ClassNotFoundException | InterruptedException e) {
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
    public String getShared_dir(){
        return shared_dir;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
