package Peers;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class peerServer implements Runnable {

    private String shared_dir;
    private int port;
    ServerSocket server;
    private HashMap<Message, Socket> requests = new HashMap();

    public peerServer(ServerSocket server, String shared_dir) {
        this.server = server;
        this.shared_dir = shared_dir;
    }

    public void run() {
        try {

            Thread daemonThread = new Thread(() -> {
                // Daemon thread runs always in the background
                while(true) {
                    synchronized (requests) {

                        try {
                            // Wait() until a download request is sent
                            requests.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        long startTime = System.currentTimeMillis();

                        // Wait until 200 ms have passed
                        while (true) {
                            if (System.currentTimeMillis() - startTime >= 200) {
                                break;
                            }
                        }

                        // Start the thread to initiate Collaborative download.
                        new Thread(new ColabDownloadHandler(requests, this.shared_dir)).start();
                    }
                }

            });
            daemonThread.setDaemon(true);
            daemonThread.start();
            while (true) {
                Socket client = this.server.accept();
                new Thread(new ClientHandler(client, this.shared_dir, this.requests)).start();
            }

        } catch (IOException e) {
            System.out.println("Server is closed...");
        }

    }

    //TODO
    //First implementation for the sake of seeder-serve method
    class SeederHandler implements Runnable {
        private Socket socket;
        private String sharedDir;

        public SeederHandler(Socket socket, String sharedDir) {
            this.socket = socket;
            this.sharedDir = sharedDir;
        }

        @Override
        public void run() {
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

                List<Message> requests = new ArrayList<>();
                long startTime = System.currentTimeMillis();

                while (System.currentTimeMillis() - startTime < 200) {
                    try {
                        Object obj = ois.readObject();
                        if (obj instanceof Message) {
                            Message msg = (Message) obj;
                            if (msg.getType() == MessageType.DOWNLOAD_REQUEST) {
                                requests.add(msg);
                            }
                        }
                    } catch (EOFException | SocketTimeoutException e) {
                        break; // Stop waiting for requests if end of stream or timeout occurs
                    }
                }
                //the seeder picks randomly which peer he will send one of the requested file fragments
                if (!requests.isEmpty()) {
                    Random random = new Random();
                    Message selectedRequest = requests.get(random.nextInt(requests.size()));
                    sendFilePiece(selectedRequest, oos);
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void sendFilePiece(Message request, ObjectOutputStream oos) throws IOException {
            String fileName = request.getContent();
            Path filePath = Paths.get(sharedDir, fileName);
            if (Files.exists(filePath)) {
                byte[] fileContent = Files.readAllBytes(filePath);
                Message response = new Message(MessageType.FILE_RESPONSE);
                response.setFileContent(fileContent);
                oos.writeObject(response);
            } else {
                Message response = new Message(MessageType.ERROR, "Requested file piece not found");
                oos.writeObject(response);
            }
        }
    }
}
