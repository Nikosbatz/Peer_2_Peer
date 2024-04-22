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
    private int countFailures ;    public PeerInfo(String peerId, List<String> files,String username,String password) {
        this.peerId = peerId;
        this.files = files;
        this.username = username;
        this.password = password;
        this.token = null;  // Initially, there is no token until login is successful
        // Initially, there is no token until login is successful
        countDownloads=0;
        countFailures=0;
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
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public static String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
    public int getCount_failures() {return countFailures;}

    public void setCount_failures(int count_failures) {this.countFailures = count_failures;}

    public int getCount_downloads() {return countDownloads;}

    public void setCount_downloads(int count_downloads) {this.countDownloads = count_downloads;}
}
}