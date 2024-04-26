package Peers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable{

    Socket client;
    public ClientHandler(Socket client){
        this.client = client;
    }



    public void run(){


        try{
            ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(client.getInputStream());

            Object msg = is.readObject();
            if (msg instanceof Message){

                switch (((Message) msg).getType()){
                    case CHECK_ACTIVE:
                        oos.writeObject(new Message(MessageType.ACTIVE_RESPONSE));

                    case DOWNLOAD_REQUEST:
                        // Invoke download()

                }
            }

        }
        catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }


    }

}
