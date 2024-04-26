package Peers;

import java.io.IOException;
import java.net.*;

public class peerServer implements Runnable{




    public void run(){
        try{
            while(true) {
                ServerSocket server = new ServerSocket(1111);
                Socket client = server.accept();

                new Thread(new ClientHandler(client)).start();
            }

        }
        catch (IOException e){
            e.printStackTrace();
        }

    }
}
