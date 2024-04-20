package Tracker;

import java.net.Socket;

public class TrackerThread implements Runnable{
    private Socket client;



    public TrackerThread(Socket client){
        this.client = client;
    }


    @Override
    public void run() {

    }
}
