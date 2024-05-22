package Peers;

import java.util.ArrayList;
import java.util.Random;

public class ColabDownloadHandler implements  Runnable{

    private ArrayList<Message> requests;

    public ColabDownloadHandler(ArrayList<Message> requests){
        this.requests = requests;
    }

    @Override
    public void run() {

        if (this.requests.size() == 1){
            handleSingleRequest();
        }
        else {

        }

    }

    private void handleSingleRequest(){

        Random random = new Random();
        ArrayList<String> fragments = requests.getLast().getFileFragments();
        String selectedFragment = fragments.get(random.nextInt(fragments.size()));


    }


}
