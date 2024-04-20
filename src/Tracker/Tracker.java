package Tracker;

import java.io.IOException;
import java.net.ServerSocket;
import java.io.*;
import java.net.*;

public class Tracker {



    public static void main(String[] args){

        System.out.println("Server running...");

        try {
            ServerSocket server = new ServerSocket(1111);

            while (true) {

                Socket socket = server.accept();
                new Thread(new TrackerThread(socket)).start();

            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
