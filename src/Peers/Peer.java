package Peers;

import java.io.*;
import java.net.*;

public class Peer {



    public static void main(String[] args){

        try {
            Socket socket = new Socket("localhost", 1111);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            System.out.println("Connected to Tracker");

        }
        catch (IOException e){
            e.printStackTrace();
        }



    }
}
