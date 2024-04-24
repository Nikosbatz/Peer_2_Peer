package Peers;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    private MessageType type;  // Enum for message type
    private String token;     // Identifier of the peer
    private ArrayList<String> files;  // List of files for file operations
    private String content;    // General content of the message


    // Constructor for messages that include peer ID and file list
    public Message(MessageType type,  ArrayList<String> files) {
        this.type = type;
        this.files = files;
    }

    // Constructor for messages that include only type and content (e.g., for login)
    public Message(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }

    // Constructor for messages with only a type (e.g., request list of files)
    public Message(MessageType type) {
        this.type = type;
    }

    // Getters and setters omitted for brevity
    public MessageType getType() {
        return type;
    }

    public String getToken() {
        return token;
    }


    public String getContent() {
        return content;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setFiles(ArrayList<String> files) {
        this.files = files;
    }
    public ArrayList<String> getFiles(){return this.files;}

    public void setContent(String content) {
        this.content = content;
    }
}
