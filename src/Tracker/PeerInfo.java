package Tracker;
import java.io.Serializable;
import java.util.List;

public class PeerInfo implements Serializable {

    private List<String> files;
    private String username;
    private String password;
    //session token
    private String token;
    // Count Total Download requests
    private int count_downloads;
    // Count Total Download request failures
    private int count_failures;
    public PeerInfo( List<String> files,String username,String password) {

        this.files = files;
        this.username = username;
        this.password = password;
        this.token = null;
        this.count_downloads = 0;
        this.count_failures = 0;

    }




    // Just Getters and Setters Below
    public List<String> getFiles() {
        return files;
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

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getCount_failures() {return count_failures;}

    public void setCount_failures(int count_failures) {this.count_failures = count_failures;}

    public int getCount_downloads() {return count_downloads;}

    public void setCount_downloads(int count_downloads) {this.count_downloads = count_downloads;}
}