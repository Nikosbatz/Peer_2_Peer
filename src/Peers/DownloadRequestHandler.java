package Peers;

import Tracker.PeerInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class DownloadRequestHandler implements Runnable{

    private PeerInfo peer;
    private String fileName;
    private Peer thisPeer;
    private ArrayList<MessageType> downloadResults;

    public DownloadRequestHandler(PeerInfo peer, String fileName, Peer thisPeer, ArrayList<MessageType> downloadResults){
        this.peer = peer;
        this.fileName = fileName;
        this.thisPeer = thisPeer;
        this.downloadResults = downloadResults;
    }

    @Override
    public void run() {

        try {
            Socket socket = new Socket(this.peer.getIp(), this.peer.getPort());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());



            // ------------- Finding missing fragments of the file -------------
            // Request the details of this file
            // TO GET THE FRAGMENTS THAT ASSEMBLE IT
            Message requestDetails = new Message(MessageType.DETAILS, fileName);
            oos.writeObject(requestDetails);

            // Get the response of the file details
            Message response =(Message) ois.readObject();

            // Get the total fragments of this file
            ArrayList<String> totalFragments = response.getFragments().get(fileName);
            // Local shared_dir files
            ArrayList<String> localFiles = this.thisPeer.getSharedDirectoryInfo();
            // To store the names of the missing fragments
            ArrayList<String> missingFragments = new ArrayList<>();
            // Find the missing fragments
            for (String fragment: totalFragments){
                if (!localFiles.contains(fragment)){
                    missingFragments.add(fragment);
                }
            }
            //------------- end of finding missing fragments -------------



            // Construct download request based on the missing fragments of the file
            Message msg = new Message(MessageType.DOWNLOAD_REQUEST, this.fileName);
            HashMap<String, ArrayList<String>> fragments = new HashMap<>();
            fragments.put(fileName,missingFragments);
            msg.setFragments(fragments);

            // Send Download request to Peer
            oos.writeObject(msg);


            Message reply = (Message) ois.readObject();

            if (reply.getType() == MessageType.FILE_RESPONSE){
                Path downloadPath = Paths.get(System.getProperty("user.dir"), "src\\"+thisPeer.getShared_dir());
                Files.createDirectories(downloadPath);
                // fileResponse.getContent contains the name of the file that the seeder sent
                downloadPath = downloadPath.resolve(reply.getContent());
                Files.write(downloadPath, reply.getFileContent());
                System.out.println("File downloaded successfully to " + downloadPath.toString());
                downloadResults.add(MessageType.NOTIFY_SUCCESS);

            }
            else if(reply.getType() == MessageType.NOTIFY_FAIL){
                downloadResults.add(MessageType.NOTIFY_FAIL);

            }



        } catch (IOException| ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


    }

}
