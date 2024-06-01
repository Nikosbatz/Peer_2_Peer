package Peers;

import Tracker.PeerInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.ArrayList;
import java.util.HashMap;

public class DownloadRequestHandler implements Runnable{

    private PeerInfo peer;
    private String fileName;
    private Peer self;
    private ArrayList<MessageType> downloadResults;
    private Message fileDetails;

    public DownloadRequestHandler(PeerInfo peer, String fileName, Peer self, ArrayList<MessageType> downloadResults, Message fileDetails){
        this.peer = peer;
        this.fileName = fileName;
        this.self = self;
        this.downloadResults = downloadResults;
        this.fileDetails = fileDetails;
    }

    @Override
    public void run() {

        try {
            // Open streams to the receiver Peer of the download Request
            Socket socket = new Socket(this.peer.getIp(), this.peer.getPort());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // ------------- Finding missing fragments of the file -------------


            // Get the total fragments of this file
            ArrayList<String> totalFragments = fileDetails.getFragments().get(fileName);
            // Local shared_dir files
            ArrayList<String> localFiles = this.self.getSharedDirectoryInfo();
            // To store the names of the missing fragments
            ArrayList<String> missingFragments = new ArrayList<>();
            // Find the missing fragments
            for (String fragment: totalFragments){
                if (!localFiles.contains(fragment)){
                    missingFragments.add(fragment);
                }
            }
            //------------- end of finding missing fragments -------------


            if (!missingFragments.isEmpty()) {

                // Construct download request based on the missing fragments of the file
                Message msg = new Message(MessageType.DOWNLOAD_REQUEST, this.fileName);
                HashMap<String, ArrayList<String>> fragments = new HashMap<>();
                fragments.put(fileName, missingFragments);
                msg.setFragments(fragments);
                msg.setUsername(self.getUsername());

                // Send Download request to Peer
                oos.writeObject(msg);
                Message reply = (Message) ois.readObject();


                if (reply.getType() == MessageType.FILE_RESPONSE) {
                    Path downloadPath = Paths.get(System.getProperty("user.dir"), "src\\" + self.getShared_dir());
                    Files.createDirectories(downloadPath);
                    // fileResponse.getContent contains the name of the file that the seeder sent
                    downloadPath = downloadPath.resolve(reply.getContent());
                    Files.write(downloadPath, reply.getFileContent());
                    System.out.println("File downloaded successfully to " + downloadPath.toString());
                    self.updateFilePartsReceivedFrom(fileName, peer.getUsername());

                } else if (reply.getType() == MessageType.NOTIFY_FAIL) {

                    System.out.println("Request was dropped");
                } else {
                    System.out.println("Message Type download request: " + reply.getType());
                }
            }

        } catch (IOException| ClassNotFoundException e) {
            e.printStackTrace();
        }


    }

}
