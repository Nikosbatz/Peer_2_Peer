package Peers;

import Tracker.PeerInfo;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ColabDownloadHandler implements  Runnable {

    private final ConcurrentHashMap<String, ArrayList<String>> filePartsReceivedFrom;
    private ArrayList<RequestInfo> requests;
    private ObjectOutputStream oos;
    private Peer peer;
    private ObjectInputStream ois;
    private String shared_dir;

    public ColabDownloadHandler(ArrayList<RequestInfo> requests, String shared_dir, Peer peer) {

        synchronized (requests) {
            // Copy references of RequestInfo objects to another ArrayList
            this.requests = new ArrayList<>();
            this.requests.addAll(requests);
            // -------- Copy end -----------
            requests.notify();
        }
        this.shared_dir = shared_dir;
        this.filePartsReceivedFrom = peer.filePartsReceivedFrom;
        this.peer = peer;
    }

    @Override
    public void run() {

        if (this.requests.size() == 1) {
            try {
                handleSingleRequest();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
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
        for (String key : msg.getFragments().keySet()) {
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
        requestMissingParts(file, requests.getLast().peerUsername);

    }

    private void handleMultipleRequests() throws IOException, InterruptedException, ClassNotFoundException {
        Random random = new Random();

        System.out.println("---------  MULTIPLE REQUESTS --------------");


        double decision = random.nextDouble();
        RequestInfo selectedRequestInfo;

        System.out.println("Decision: " + decision);

        if (decision <= 0.2) {
            selectedRequestInfo = requests.get(random.nextInt(requests.size()));
            System.out.println("Random request");
        } else if (decision <= 0.6) {
            selectedRequestInfo = getBestRequest(requests);
            System.out.println("Best Request");
        } else {
            selectedRequestInfo = getFrequentRequest(requests);
            System.out.println("Frequent Request");
        }

        System.out.println("Request reference: " + selectedRequestInfo);

        initObjectStreams(selectedRequestInfo);



        /*String selectedFragment = selectedRequestInfo.msg.getFragments()
                .get(selectedRequestInfo.msg.getContent())
                .get(random.nextInt(selectedRequestInfo.msg.getFragments().get(selectedRequestInfo.msg.getContent()).size()));*/


        // Fragments of the file that the client requests
        ArrayList<String> fragments = null;
        String file = "";

        // Retrieve the only pair from the hashmap
        Message msg = selectedRequestInfo.msg;
        for (String key : msg.getFragments().keySet()) {
            fragments = msg.getFragments().get(key);
            file = key;
        }

        // Get a random fragment from those requested
        String selectedFragment = fragments.get(random.nextInt(fragments.size()));

        initUpload(selectedFragment);

        requestMissingParts(file, selectedRequestInfo.peerUsername);


        requests.remove(selectedRequestInfo);
        for (RequestInfo requestInfo : requests) {
            if (!requestInfo.peerUsername.equals(selectedRequestInfo.peerUsername)) {
                sendNegativeResponse(requestInfo);
            }
        }

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
        System.out.println(response.getType());
        return response.getPeers().getLast(); // Ensure this returns the necessary peer info details
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


    private void sendNegativeResponse(RequestInfo request) throws IOException {
        Message response = new Message(MessageType.NOTIFY_FAIL, "The Request was dropped");
        request.oos.writeObject(response);
    }

    private void requestMissingParts(String fileName, String username) throws IOException, ClassNotFoundException {
        ArrayList<String> missingFragments = getMissingParts(fileName);
        System.out.println(missingFragments.size());

        if (!missingFragments.isEmpty()) {

            Message request = new Message(MessageType.DOWNLOAD_REQUEST, fileName);
            HashMap<String, ArrayList<String>> fragments = new HashMap<>();
            fragments.put(fileName, missingFragments);
            request.setFragments(fragments);
            request.setUsername(peer.getUsername());
            oos.writeObject(request);

            Message reply = (Message) ois.readObject();
            if (reply.getType() == MessageType.FILE_RESPONSE) {
                Path downloadPath = Paths.get(System.getProperty("user.dir"), "src\\" + peer.getShared_dir());
                Files.createDirectories(downloadPath);
                // fileResponse.getContent contains the name of the file that the seeder sent
                downloadPath = downloadPath.resolve(reply.getContent());
                Files.write(downloadPath, reply.getFileContent());
                System.out.println("File downloaded successfully to " + downloadPath.toString());
                peer.updateFilePartsReceivedFrom(fileName, username);

            } else {
                System.out.println("THERE ARE NO MISSING FRAGMENTS");
            }

        }
    }

        private ArrayList<String> getMissingParts (String fileName) throws IOException, ClassNotFoundException {


            ArrayList<String> totalFragments;

            totalFragments = requestFileFragments(fileName);

            ArrayList<String> files = peer.getSharedDirectoryInfo();
            ArrayList<String> missingFragments = new ArrayList<>();


            for (String fragment : totalFragments) {
                if (!files.contains(fragment)) {
                    missingFragments.add(fragment);
                }
            }

            return missingFragments;


        }


        private ArrayList<String> requestFileFragments (String fileName) throws IOException, ClassNotFoundException {

            Message requestDetails = new Message(MessageType.DETAILS, fileName);
            Message response;
            synchronized (peer.getOos()) {

                peer.getOos().writeObject(requestDetails);
                response = (Message) peer.getOis().readObject();

            }
            return response.getFragments().get(fileName);

        }

        private RequestInfo getFrequentRequest (ArrayList < RequestInfo > requests) throws IOException, ClassNotFoundException {

            int maxDownloads = 0;
            RequestInfo maxRequest = null;

            for (RequestInfo request : requests) {
                String username = request.peerUsername;
                if (peer.filePartsReceivedFrom.containsKey(username) && peer.filePartsReceivedFrom.get(username).size() > maxDownloads) {
                    maxDownloads = peer.filePartsReceivedFrom.get(username).size();
                    maxRequest = request;
                }

            }
            if (maxRequest == null) {
                return getBestRequest(requests);
            } else {
                return maxRequest;
            }

        }




}
