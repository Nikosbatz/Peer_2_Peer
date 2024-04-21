package Peers;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private MessageType type;  // Enum for message type
    private String peerId;     // Identifier of the peer
    private List<String> files;  // List of files for file operations
    private String content;    // General content of the message

    // Constructor for messages that include peer ID and file list
    public Message(MessageType type, String peerId, List<String> files) {
        this.type = type;
        this.peerId = peerId;
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

    public String getPeerId() {
        return peerId;
    }

    public List<String> getFiles() {
        return files;
    }

    public String getContent() {
        return content;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
