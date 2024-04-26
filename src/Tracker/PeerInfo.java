package Tracker;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PeerInfo implements Serializable {

    private ArrayList<String> files;
    private String username;
    private String password;
    private String token;//session token to manage logins and logout



    private int port;
    private String ip;
    private int countDownloads ;
    private int countFailures ;
    private long lastHeartbeat;  // Timestamp of the last heartbeat

    public PeerInfo(String username,String password) {

        this.username = username;
        this.password = password;
        // Initially, there is no token until login is successful
        this.token = null;
        this.lastHeartbeat=0;
        this.countDownloads=0;
        this.countFailures=0;
    }

    // Getters

    public ArrayList<String> getFiles() {
        return files;
    }

    // Setters

    public void setFiles(ArrayList<String> files) {
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

    public void incCountfailures() {this.countFailures ++;}

    public int getCountDownloads() {return countDownloads;}

    public void incCountDownloads() {this.countDownloads++;}

    public void setLastHeartbeat(long currentTime) {this.lastHeartbeat=currentTime;}

    public long getLastHeartbeat() {
        return this.lastHeartbeat;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
