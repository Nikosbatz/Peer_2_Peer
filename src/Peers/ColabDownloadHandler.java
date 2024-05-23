package Peers;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ColabDownloadHandler implements  Runnable{

    private HashMap<Message, Socket> requests;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String shared_dir;
    private Map<String, HashMap<Integer, String>> filePartsReceivedFrom;
    public ColabDownloadHandler(HashMap<Message, Socket> requests, String shared_dir){
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

        }

    }

    // In case there is only 1 Request in the requests buffer
    private void handleSingleRequest() throws IOException {

        Random random = new Random();
        Socket socket = null;
        Message request = null;

        // Retrieve the only pair from the hashmap
        for (Message key: requests.keySet()){
            socket = requests.get(key);
            request = key;
        }

        // Fragments of the file that the client requests
        ArrayList<String> fragments = null;
        String file;

        // Retrieve the only pair from the hashmap
        for (String key: request.getFragments().keySet()){
            fragments = request.getFragments().get(key);
            file = key;
        }

        String selectedFragment = fragments.get(random.nextInt(fragments.size()));

        // For debugging
        System.out.println(selectedFragment);
        //

        initObjectStreams(socket);
        initUpload(selectedFragment);


    }

    private void initObjectStreams(Socket socket) throws IOException {
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.ois = new ObjectInputStream(socket.getInputStream());
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
                oos.writeObject(new Message(MessageType.FILE_RESPONSE, fileContent));  // Send file content as a message
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
        return 10; //  placeholder
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
