package Peers;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientHandler implements Runnable {

    Socket client;
    String shared_dir;
    HashMap<Message, Socket> requests;

    public ClientHandler(Socket client, String shared_dir, HashMap<Message, Socket> requests) {

        this.client = client;
        this.shared_dir = shared_dir;
        this.requests = requests;
    }


    public void run() {


        try {
            ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(client.getInputStream());

            Object msg = is.readObject();
            if (msg instanceof Message) {

                switch (((Message) msg).getType()) {
                    case CHECK_ACTIVE:
                        oos.writeObject(new Message(MessageType.ACTIVE_RESPONSE));
                        break;

                    case DOWNLOAD_REQUEST:
                        synchronized (requests) {
                            requests.put((Message) msg, client);
                            requests.notify();
                        }

                        //handleDownloadRequest(((Message) msg).getContent(), oos);
                        break;
                    default:
                        System.out.println("Received an unrecognized message type.");
                        break;

                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }


    }

    private void handleCheckActive(ObjectOutputStream oos) throws IOException {
        // Respond back with ACTIVE_RESPONSE indicating this peer is active
        oos.writeObject(new Message(MessageType.ACTIVE_RESPONSE));
    }

    private void handleDownloadRequest(String fileName, ObjectOutputStream oos) throws IOException {


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
