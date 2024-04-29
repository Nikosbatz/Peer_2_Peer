package Peers;

import java.io.IOException;
import java.net.*;

public class peerServer implements Runnable{

    private String shared_dir;
    private int port;
    ServerSocket server;

    public peerServer(ServerSocket server, String shared_dir){
        this.server = server;
        this.shared_dir = shared_dir;
    }

    public void run(){
        try{

            while(true) {
                Socket client = this.server.accept();
                new Thread(new ClientHandler(client, this.shared_dir)).start();
            }

        }
        catch (IOException e){
            System.out.println("Server is closed...");
        }

    }
}
