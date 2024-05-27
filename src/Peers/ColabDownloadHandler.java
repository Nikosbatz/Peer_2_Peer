package Peers;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ColabDownloadHandler implements  Runnable{

    private ArrayList<RequestInfo> requests;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String shared_dir;
    //the outer part uses the filename that is being downloaded as the key ,the inner one uses the part number as the key
    // and the username of the peer who sent that part as the value,this is used to keep track of the parts of the file
    //and the peer from whom they were recieved
    private Map<String, HashMap<Integer, String>> filePartsReceivedFrom;
    public ColabDownloadHandler(ArrayList<RequestInfo> requests, String shared_dir){
        this.requests = requests;
        this.shared_dir = shared_dir;
        this.filePartsReceivedFrom = filePartsReceivedFrom;

    }

    @Override
    public void run() {

        if (this.requests.size() == 1){
            try {
                handleSingleRequest();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try {
                handleMultipleRequests();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    // In case there is only 1 Request in the requests buffer
    private void handleSingleRequest() throws IOException {

        Random random = new Random();
        Socket socket = null;
        Message msg = null;

        // Retrieve the only pair from the hashmap
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
        requestMissingParts(requests.get(0).peerUsername, file);


    }
    private void handleMultipleRequests() throws IOException, InterruptedException {
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





    // ------- ANESTIS ---------------
    private void sendNegativeResponse() throws IOException {
        Message response= new Message(MessageType.ERROR,"Requested file not available.");
        oos.writeObject(response);
    }

    private void requestMissingParts(String peerUsername, String fileName) throws IOException {
        ArrayList<String> missingParts = getMissingParts(fileName);
        if (!missingParts.isEmpty()) {
            Message request = new Message(MessageType.DOWNLOAD_REQUEST, fileName);
            request.setContent(missingParts.get(new Random().nextInt(missingParts.size())));
            oos.writeObject(request);
        }
    }
    private ArrayList<String> getMissingParts(String fileName) {
        int totalParts = getTotalPartsCount(fileName);
        ArrayList<String> missingParts = new ArrayList<>();

        HashMap<Integer, String> receivedParts = filePartsReceivedFrom.getOrDefault(fileName, new HashMap<>());
        for (int i = 0; i < totalParts; i++) {
            if (!receivedParts.containsKey(i)) {
                missingParts.add(fileName + ".part" + i);
            }
        }
        return missingParts;
    }
    private int getTotalPartsCount(String fileName) {
        // each part is 1MB
        Path filePath = Paths.get(this.shared_dir, fileName);
        if (Files.exists(filePath)) {
            try {
                long fileSize = Files.size(filePath);
                int partSize = 1024 * 1024; // 1MB
                return (int) Math.ceil((double) fileSize / partSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0; //  placeholder
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
