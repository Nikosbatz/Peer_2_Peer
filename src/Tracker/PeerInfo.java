package Tracker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PeerInfo implements Serializable {
    private ArrayList<String> files;
    private HashMap<String, Boolean> isFileInitSeeder = new HashMap<>();
    private String username;
    private String password;
    private String token; // session token to manage logins and logout
    private int port;
    private String ip;
    private int countDownloads;
    private int countFailures;
    private double score;
    private List<Integer> pieces; // List to store pieces of the file the peer has
    private boolean seederBit; // Indicates if the peer is the original seeder

    public PeerInfo(String username, String password) {
        this.username = username;
        this.password = password;
        this.token = null;
        this.countDownloads = 0;
        this.countFailures = 0;
        this.pieces = new ArrayList<>();
        this.seederBit = false;
    }

    // Getters
    public ArrayList<String> getFiles() {
        return files;
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

    public int getPort() {
        return port;
    }

    public double getScore() {
        return score;
    }

    public int getCountFailures() {
        return countFailures;
    }

    public int getCountDownloads() {
        return countDownloads;
    }

    public String getIp() {
        return ip;
    }

    public List<Integer> getPieces() {
        return pieces;
    }

    public boolean isSeeder() {
        return seederBit;
    }

    // Setters
    public void setFiles(ArrayList<String> files) {
        this.files = files;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setScore(double score) {
        this.score = score;
    }
    public void incCountDownloads() {
        this.countDownloads++;
    }
    public void incCountfailures() {
        this.countFailures++;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPieces(List<Integer> pieces) {
        this.pieces = pieces;
    }

    public void setSeeder(boolean seederBit) {
        this.seederBit = seederBit;
    }


    public HashMap<String, Boolean> getIsFileInitSeeder() {
        return isFileInitSeeder;
    }

    public void setIsFileInitSeeder(HashMap<String, Boolean> isFileInitSeeder) {
        this.isFileInitSeeder = isFileInitSeeder;
    }
}
