package Peers;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class ColabDownloadHandler implements  Runnable{

    private HashMap<Message, Socket> requests;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String shared_dir;

    public ColabDownloadHandler(HashMap<Message, Socket> requests, String shared_dir){
        this.requests = requests;
        this.shared_dir = shared_dir;
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

    private void handleSingleRequest() throws IOException {

        Random random = new Random();
        Socket socket = null;
        Message request = null;

        // Retrieve the only pair from the hashmap
        for (Message key: requests.keySet()){
            socket = requests.get(key);
            request = key;
        }

        ArrayList<String> fragments = request.getFileFragments();
        String selectedFragment = fragments.get(random.nextInt(fragments.size()));

        // For debugging
        System.out.println(selectedFragment);
        //

        initObjectStreams(socket);
        handleDownloadRequest(selectedFragment);


    }

    private void initObjectStreams(Socket socket) throws IOException {
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.ois = new ObjectInputStream(socket.getInputStream());
    }


    private void handleDownloadRequest(String fileName) throws IOException {



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


}
