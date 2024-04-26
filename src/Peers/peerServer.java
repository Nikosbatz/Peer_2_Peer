package Peers;

import java.io.IOException;
import java.net.*;

public class peerServer implements Runnable{




    public void run(){
        try{

            ServerSocket server = new ServerSocket(1112);

            while(true) {
                Socket client = server.accept();
                new Thread(new ClientHandler(client)).start();
            }

        }
        catch (IOException e){
            e.printStackTrace();
        }

    }
}
