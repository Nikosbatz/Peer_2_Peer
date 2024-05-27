package Peers;

import Tracker.PeerInfo;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ColabDownloadHandler implements  Runnable{

    private ArrayList<RequestInfo> requests;
    private ObjectOutputStream oos;
    private Peer peer;
    private ObjectInputStream ois;
    private String shared_dir;
    //the outer part uses the filename that is being downloaded as the key ,the inner one uses the part number as the key
    // and the username of the peer who sent that part as the value,this is used to keep track of the parts of the file
    //and the peer from whom they were recieved
    private Map<String, HashMap<Integer, String>> filePartsReceivedFrom;
    public ColabDownloadHandler(ArrayList<RequestInfo> requests, String shared_dir, Peer peer){
        this.requests = requests;
        this.shared_dir = shared_dir;
        this.filePartsReceivedFrom = filePartsReceivedFrom;
        this.peer = peer;
    }

    @Override
    public void run() {

        if (this.requests.size() == 1){
            try {
                handleSingleRequest();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        /*else {
            try {
                handleMultipleRequests();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }*/

    }

    // In case there is only 1 Request in the requests buffer
    private void handleSingleRequest() throws IOException, ClassNotFoundException {

        Random random = new Random();
        Socket socket = null;
        Message msg = null;

        // Retrieve the only request
        msg = requests.getLast().msg;


        // Fragments of the file that the client requests
        ArrayList<String> fragments = null;
        String file = "";

        // Retrieve the only pair from the hashmap
        for (String key: msg.getFragments().keySet()){
            fragments = msg.getFragments().get(key);
            file = key;
        }

        // Get a random fragment from those requested
        String selectedFragment = fragments.get(random.nextInt(fragments.size()-1));

        // For debugging
        System.out.println(selectedFragment);
        //

        // Init output and input streams to the client Peer
        initObjectStreams(requests.getLast());

        //upload the random fragment to the client Peer
        initUpload(selectedFragment);

        // Request missing parts from the peer
        requestMissingParts(file);


    }
    /*private void handleMultipleRequests() throws IOException, InterruptedException {
        Random random = new Random();
        Thread.sleep(200); // Wait for 200ms to gather multiple requests if any

        double decision = random.nextDouble();
        RequestInfo selectedRequestInfo;

        if (decision <= 0.2) {
            selectedRequestInfo = requests.get(random.nextInt(requests.size()));
        } else if (decision <= 0.6) {
            selectedRequestInfo = getBestRequest(requests);
        } else {
            selectedRequestInfo = getFrequentRequest(requests);
        }

        initObjectStreams(selectedRequestInfo);
        String selectedFragment = selectedRequestInfo.msg.getFragments()
                .get(selectedRequestInfo.msg.getContent())
                .get(random.nextInt(selectedRequestInfo.msg.getFragments().get(selectedRequestInfo.msg.getContent()).size()));
        handleDownloadRequest(selectedFragment);

        for (RequestInfo requestInfo : requests) {
            if (!requestInfo.peerUsername.equals(selectedRequestInfo.peerUsername)) {
                sendNegativeResponse(requestInfo.oos);
            }
        }

        // Request missing parts from the selected peer
        requestMissingParts(selectedRequestInfo.peerUsername, selectedRequestInfo.msg.getContent());
    }*/

    private void initObjectStreams(RequestInfo request) throws IOException {
        this.oos = request.oos;
        this.ois = request.ois;
    }


    private void initUpload(String fileName) throws IOException {

        Path dir = Paths.get(System.getProperty("user.dir")).resolve("src");
        String path = dir.resolve(this.shared_dir).toString() + File.separator + fileName;

        File file = new File(path);

        if (file.exists() && file.isFile()) {
            // Read the file content and send it
            byte[] fileContent = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(fileContent);  // Read file into byte array
                Message response = new Message(MessageType.FILE_RESPONSE, fileContent);
                response.setContent(fileName);
                oos.writeObject(response);  // Send file content as a message
            }
        } else {
            // File not found or not accessible
            oos.writeObject(new Message(MessageType.ERROR, "File not found or inaccessible."));
        }
    }





    // ------- ANESTIS ---------------
    private void sendNegativeResponse() throws IOException {
        Message response= new Message(MessageType.ERROR,"Requested file not available.");
        oos.writeObject(response);
    }

    private void requestMissingParts(String fileName) throws IOException, ClassNotFoundException {
        ArrayList<String> missingFragments = getMissingParts(fileName);
        System.out.println(missingFragments.size());
        if (!missingFragments.isEmpty()) {

            Message request = new Message(MessageType.DOWNLOAD_REQUEST, fileName);
            HashMap<String, ArrayList<String>> fragments = new HashMap<>();
            fragments.put(fileName, missingFragments);
            request.setFragments(fragments);
            oos.writeObject(request);
        }
        else {
            System.out.println("THERE NO MISSING FRAGMENTS");
        }

    }
    private ArrayList<String> getMissingParts(String fileName) throws IOException, ClassNotFoundException {


        ArrayList<String> totalFragments;

        totalFragments = requestFileFragments(fileName);

        ArrayList<String> files = peer.getSharedDirectoryInfo();
        ArrayList<String> missingFragments = new ArrayList<>();


        for (String fragment: totalFragments){
            if (!files.contains(fragment)){
                missingFragments.add(fragment);
            }
        }

        return missingFragments;


    }


    private ArrayList<String> requestFileFragments(String fileName) throws IOException, ClassNotFoundException {

        Message requestDetails = new Message(MessageType.DETAILS, fileName);
        peer.getOos().writeObject(requestDetails);


        Message response =(Message) peer.getOis().readObject();
        return response.getFragments().get(fileName);

    }

    private Message getBestRequest(ArrayList<Message> requests) {
        //  the request with the highest priority or from the peer with the best performance
        return requests.get(0); // Placeholder return
    }

    private Message getFrequentRequest(ArrayList<Message> requests) {
        HashMap<String, Integer> peerRequestCount = new HashMap<>();
        for (Message request : requests) {
            peerRequestCount.put(request.getUsername(), peerRequestCount.getOrDefault(request.getUsername(), 0) + 1);
        }

        String frequentPeer = Collections.max(peerRequestCount.entrySet(), Map.Entry.comparingByValue()).getKey();
        for (Message request : requests) {
            if (request.getUsername().equals(frequentPeer)) {
                return request;
            }
        }
        return requests.get(0); // Fallback to the first request
    }
}
