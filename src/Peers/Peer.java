package Peers;

import java.io.*;
import java.net.*;
import java.util.*;

public class Peer {
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String token;

    public Peer(String trackerHost, int trackerPort) throws IOException {
        socket = new Socket(trackerHost, trackerPort);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public void registerWithTracker( List<String> files) throws IOException, ClassNotFoundException {
        Message msg = new Message(MessageType.REGISTER, files);

        //TODO Ask user to input USERNAME and PASSWORD for the registration
        msg.setContent("123:123");

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
        // Create the login message with appropriate type and concatenated content of username and password
        Message loginMessage = new Message(MessageType.LOGIN, username + ":" + password);
        oos.writeObject(loginMessage); // Send login message to the tracker
        Object response = ois.readObject(); // Read response from the tracker

        if (response instanceof Message) {
            Message responseMessage = (Message) response;
            if (responseMessage.getType() == MessageType.LOGIN_SUCCESS) {
                this.token = responseMessage.getToken();
                System.out.println("Login successful. Token: " + this.token);

            } else {
                System.out.println("Login failed.");
            }
        }
    }

    public void logOut() throws IOException, ClassNotFoundException{
        Message msg = new Message(MessageType.LOGOUT);
        msg.setToken(this.token);
        oos.writeObject(msg);
        Object reply = ois.readObject();
        if (reply instanceof Message){
            System.out.println(((Message) reply).getContent());
        }
    }


    public static void main(String[] args) {
        try {
            Peer peer = new Peer("localhost", 1111);

            peer.registerWithTracker( Arrays.asList("file1.txt", "file2.txt"));
            peer.listFiles();
            peer.login("123", "123");
            peer.logOut();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
