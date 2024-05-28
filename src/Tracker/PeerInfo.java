package Tracker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PeerInfo implements Serializable {
    private ArrayList<String> files;
    private HashMap<String, Boolean> isFileInitSeeder ;
    private HashMap<String, ArrayList<String>> fragments; // List to store pieces of the file the peer has
    private String username;
    private String password;
    private String token; // session token to manage logins and logout
    private int port;
    private String ip;
    private int countDownloads;
    private int countFailures;
    private double score;

    public PeerInfo(String username, String password) {
        this.username = username;
        this.password = password;
        this.token = null;
        this.countDownloads = 0;
        this.countFailures = 0;
        this.fragments = new HashMap<>();
        this.isFileInitSeeder = new HashMap<>();
    }
    public PeerInfo() {}


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

    public HashMap<String, ArrayList<String>> getFragments() {
        return fragments;
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

    public void setFragments(HashMap<String, ArrayList<String>> fragments) {
        this.fragments = fragments;
    }


    public HashMap<String, Boolean> getIsFileInitSeeder() {
        return isFileInitSeeder;
    }

    public void setIsFileInitSeeder(HashMap<String, Boolean> isFileInitSeeder) {
        this.isFileInitSeeder = isFileInitSeeder;
    }
}
