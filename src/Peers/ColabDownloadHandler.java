package Peers;

import Tracker.PeerInfo;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ColabDownloadHandler implements  Runnable{

    private final HashMap<String, HashMap<Integer, String>> filePartsReceivedFrom;
    private ArrayList<RequestInfo> requests;
    private ObjectOutputStream oos;
    private Peer peer;
    private ObjectInputStream ois;
    private String shared_dir;
   
    public ColabDownloadHandler(ArrayList<RequestInfo> requests, String shared_dir, Peer peer){
        this.requests = requests;
        this.shared_dir = shared_dir;
        this.filePartsReceivedFrom = peer.filePartsReceivedFrom;
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
        else {
            try {
                handleMultipleRequests();
            } catch (IOException | InterruptedException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

    }

    // In case there is only 1 Request in the requests buffer
    private void handleSingleRequest() throws IOException, ClassNotFoundException {

        Random random = new Random();
        Message msg;

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
        String selectedFragment = fragments.get(random.nextInt(fragments.size()));

        // For debugging
        System.out.println(selectedFragment);
        //

        // Init output and input streams to the client Peer
        initObjectStreams(requests.getLast());

        //upload the random fragment to the client Peer
        initUpload(selectedFragment);

        // Request missing parts from the peer
        requestMissingParts(file);
       //TODO filesRecievedFrom update it


    }
    private void handleMultipleRequests() throws IOException, InterruptedException, ClassNotFoundException {
        Random random = new Random();
        Thread.sleep(200);

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

        initUpload(selectedFragment);
        // Update filePartsReceivedFrom
        updateFilePartsReceivedFrom(selectedRequestInfo.msg.getContent(), selectedFragment, selectedRequestInfo.peerUsername);

        for (RequestInfo requestInfo : requests) {
            if (!requestInfo.peerUsername.equals(selectedRequestInfo.peerUsername)) {
                sendNegativeResponse();
            }
        }

        requestMissingParts(selectedRequestInfo.msg.getContent());
    }

    //calculates the best request based on the peer's response time and a combined score of downloads and failures.
    /// Calculates the best request based on the peer's response time and a combined score of downloads and failures.
    private RequestInfo getBestRequest(ArrayList<RequestInfo> requests) throws IOException, ClassNotFoundException {
        RequestInfo bestRequest = null;
        double bestScore = Double.MAX_VALUE;

        for (RequestInfo requestInfo : requests) {
            PeerInfo peerInfo = getPeerInfoFromRequest(requestInfo);
            long responseTime = peer.checkActive(peerInfo);
            double score = responseTime * Math.pow(0.75, peerInfo.getCountDownloads()) * Math.pow(1.25, peerInfo.getCountFailures());

            if (score < bestScore) {
                bestScore = score;
                bestRequest = requestInfo;
            }
        }

        return bestRequest;
    }

    // Sends a request to get the PeerInfo for a given peer based on their username.
    private PeerInfo getPeerInfoFromRequest(RequestInfo requestInfo) throws IOException, ClassNotFoundException {
        Message peerInfoRequest = new Message(MessageType.PEER_INFO, requestInfo.peerUsername);
        peer.getOos().writeObject(peerInfoRequest);

        Message response = (Message) peer.getOis().readObject();
        return response.getPeers().get(0); // Ensure this returns the necessary peer info details
    }

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
            System.out.println("THERE ARE NO MISSING FRAGMENTS");
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

    private RequestInfo getFrequentRequest(ArrayList<RequestInfo> requests) {
        for (RequestInfo request : requests) {
            String fileName = request.msg.getContent();
            int partNumber = Integer.parseInt(request.msg.getFragments().get(fileName).get(0).split("_")[1]); //  the fragment name format is 'fileName_partNumber'
            String peerUsername = request.peerUsername;

            filePartsReceivedFrom.putIfAbsent(fileName, new HashMap<>());
            filePartsReceivedFrom.get(fileName).put(partNumber, peerUsername);
        }

        HashMap<String, Integer> requestCount = new HashMap<>();
        for (HashMap<Integer, String> parts : peer.filePartsReceivedFrom.values()) {
            for (String peerUsername : parts.values()) {
                requestCount.put(peerUsername, requestCount.getOrDefault(peerUsername, 0) + 1);
            }
        }

        String frequentPeer = Collections.max(requestCount.entrySet(), Map.Entry.comparingByValue()).getKey();
        for (RequestInfo request : requests) {
            if (request.peerUsername.equals(frequentPeer)) {
                return request;
            }
        }

        return requests.get(0); // Fallback to the first request if no frequent peer is found
    }

    private void updateFilePartsReceivedFrom(String fileName, String fragment, String peerUsername) {
        int fragmentNumber = Integer.parseInt(fragment.split("_")[1]); // the fragment name format is 'fileName_partNumber'
        filePartsReceivedFrom.putIfAbsent(fileName, new HashMap<>());
        filePartsReceivedFrom.get(fileName).put(fragmentNumber, peerUsername);
    }

}
