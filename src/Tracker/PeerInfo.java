package Tracker;
import java.io.Serializable;
import java.util.List;

public class PeerInfo implements Serializable {
    private String peerId;
    private List<String> files;
    private String username;
    private String password;
    private static String token;//session token to manage logins and logoutzzz


    private String ipAddress;
    private int port;
    private int countDownloads ;
    private int countFailures ;
    private long lastHeartbeat;  // Timestamp of the last heartbeat

    public PeerInfo(List<String> files,String username,String password) {

        this.files = files;
        this.username = username;
        this.password = password;
        this.token = null;  // Initially, there is no token until login is successful
        this.lastHeartbeat=0;
        this.countDownloads=0;
        this.countFailures=0;
    }

    // Getters
    public String getIpAddress() {
        return ipAddress;
    }

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
    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getPort() {
        return port;
    }
    public void setPort(int port){
        this.port=port;
    }
    public int getCountFailures() {return countFailures;}

    public void setCountfailures(int count_failures) {this.countFailures = count_failures;}

    public int getCountDownloads() {return countDownloads;}

    public void setCountDownloads(int count_downloads) {this.countDownloads = count_downloads;}

    public void setLastHeartbeat(long currentTime) {this.lastHeartbeat=currentTime;}

    public long getLastHeartbeat() {
        return this.lastHeartbeat;
    }
}
