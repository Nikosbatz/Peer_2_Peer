package Tracker;
import java.io.Serializable;
import java.util.List;

public class PeerInfo implements Serializable {
    private String peerId;
    private List<String> files;

    public PeerInfo(String peerId, List<String> files) {
        this.peerId = peerId;
        this.files = files;
    }

    // Getters
    public String getPeerId() {
        return peerId;
    }

    public List<String> getFiles() {
        return files;
    }

    // Setters
    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }
}