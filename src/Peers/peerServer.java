package Peers;

import java.io.IOException;
import java.net.*;

public class peerServer implements Runnable{

    private String shared_dir;
    private int port;

    public peerServer(String shared_dir, int port){
        this.shared_dir = shared_dir;
        this.port = port;
    }

    public void run(){
        try{

            ServerSocket server = new ServerSocket(this.port);

            while(true) {
                Socket client = server.accept();
                new Thread(new ClientHandler(client, this.shared_dir)).start();
            }

        }
        catch (IOException e){
            e.printStackTrace();
        }

    }
}
