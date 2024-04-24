package Peers;

import java.io.*;
import java.net.*;
import java.util.*;

public class Peer {
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String token;

    public static Boolean processRunning;

    public Peer(String trackerHost, int trackerPort) throws IOException {
        socket = new Socket(trackerHost, trackerPort);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public void registerWithTracker(String username, String password) throws IOException, ClassNotFoundException {
        Message msg = new Message(MessageType.REGISTER);

        //TODO Ask user to input USERNAME and PASSWORD for the registration

        msg.setContent(username+":"+password);

        oos.writeObject(msg);
        Object response = ois.readObject();
        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            System.out.println("Registration Response: " + responseMessage.getContent());
        }
    }

    public void listFiles() throws IOException, ClassNotFoundException {
        Message msg = new Message(MessageType.LIST_FILES);
        oos.writeObject(msg);
        Object response = ois.readObject();
        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            System.out.println("Available files: " + responseMessage.getContent());
        }
    }

    public void login(String username, String password) throws IOException, ClassNotFoundException {
        // Create the login message with username and password
        Message loginMessage = new Message(MessageType.LOGIN, username + ":" + password);
        oos.writeObject(loginMessage); // Send login message to the tracker

        // Await the response from the tracker
        Object response = ois.readObject();

        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            if (responseMessage.getType() == MessageType.LOGIN_SUCCESS) {
                // Extract token
                String[] details = responseMessage.getContent().split(",");
                this.token = details[2].split(":")[1].trim();  //  "IP Address: x.x.x.x, Port: xxxx, Token: xxx"
                String ipAddress = details[0].split(":")[1].trim();
                int port = Integer.parseInt(details[1].split(":")[1].trim());

                System.out.println("Login successful. Token: " + this.token);
                System.out.println("Assigned IP: " + ipAddress);
                System.out.println("Assigned Port: " + port);

                //  send additional information back to the tracker if needed (INFORM METHOD)
                sendAdditionalInfo();
            } else {
                System.out.println("Login failed. Reason: " + responseMessage.getContent());
            }
        } else {
            System.out.println("Unexpected response type from server.");
        }
    }


    //login help function
    private void sendAdditionalInfo() throws IOException {
        // Assuming you have methods to get the shared directory info and other necessary details

        String ip = getLocalIPAddress();
        int port = getLocalPort();

        Message additionalInfoMessage = new Message(MessageType.INFORM, ip + "," + port );
        additionalInfoMessage.setToken(this.token);
        additionalInfoMessage.setFiles(getSharedDirectoryInfo());
        oos.writeObject(additionalInfoMessage);
    }
    //TODO these are examples will implement w/t setters etc later
    private ArrayList<String> getSharedDirectoryInfo() {
        // Path for this peer's shared_directory
        String shared_dir = "C:\\Users\\nikos\\Documents\\GitHub\\Peer_2_Peer\\src\\shared_Directory1";
        //
        File dir = new File(shared_dir);

        // if directory is not empty
        if (dir.listFiles() != null){
            ArrayList<File> files = new ArrayList<>(Arrays.asList(dir.listFiles()));
            ArrayList<String> fileNames = new ArrayList<>();
            for (File file: files){
                fileNames.add(file.getName());
            }
            return fileNames;
        }
        else{
            return null;
        }

    }
    private String getLocalIPAddress() {
        return "192.168.1.1";
    }
    private int getLocalPort() {
        return 1234;
    }

    public void logOut() throws IOException, ClassNotFoundException{
        Message msg = new Message(MessageType.LOGOUT);
        msg.setToken(this.token);
        oos.writeObject(msg);

        // Wait for server reply
        Object reply = ois.readObject();

        if (reply instanceof Message){

            Message rep = (Message) reply;
            System.out.println(rep.getContent());

            // If Tracker replies with successful logout then token = null
            if (((Message) reply).getType() == MessageType.LOGOUT_SUCCESS){
                this.token = null;
            }
            else{
                System.out.println("DEN KANEI LOGOUT");
            }
        }
    }

    public void showMenu () throws IOException, ClassNotFoundException{
        Scanner in = new Scanner(System.in);

        // If peer in not connected show the login/register forms
        if (this.token == null) {
            String username;
            String password;
            System.out.print("Choose an option:\n1. Registration\n2. Login\n3. Exit\nEnter your choice: ");
            String choice = in.nextLine();

            switch (choice){
                case "1":
                    System.out.print("Username: ");
                    username = in.nextLine();
                    System.out.print("\nPassword: ");
                    password = in.nextLine();
                    registerWithTracker(username, password);
                    break;
                case "2":
                    System.out.print("Username: ");
                    username = in.nextLine();
                    System.out.print("\nPassword: ");
                    password = in.nextLine();
                    login(username, password);
                    break;
                case "3":
                    processRunning = false;
                    break;
            }
        }
        // If peer is connected show the operations menu
        else {
            //TODO make the operations menu
            System.out.print("Choose an option:\n1. Logout\nEnter your choice: ");
            String choice = in.nextLine();
            switch (choice){
                case "1":
                    logOut();
                    break;
            }

        }
    }


    public static void main(String[] args) {
        try {
            Peer peer = new Peer("localhost", 1111);
            peer.getSharedDirectoryInfo();
            processRunning = true;
            while(processRunning) {
                peer.showMenu();
            }


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
