package vttp.ssf_mini_project.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import jakarta.validation.constraints.Size;
import vttp.ssf_mini_project.util.Util;

public class User {

    @Size(min = 5, max = 30, message = "Username must be between 5 to 30 characters in length.")
    private String username;

    @Size(min = 1, message = "Please select at least one topic of interest.")
    private List<String> topicsOfInterest;

    @Size(min = 5, max = 30, message = "Password must be between 5 to 30 characters in length.")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private Deque<String> queryHist;

    public User() {
        this.queryHist = new ArrayDeque<>(Util.queryHistMaxSize);
    }

    public Deque<String> getQueryHist() {
        return queryHist;
    }
    public void setQueryHist(Deque<String> queryHist) {
        this.queryHist = queryHist;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public List<String> getTopicsOfInterest() {
        return topicsOfInterest;
    }
    public void setTopicsOfInterest(List<String> topicsOfInterest) {
        this.topicsOfInterest = topicsOfInterest;
    }
   

    public void addQuery(String query){
        if (queryHist==null){
            queryHist = new ArrayDeque<>(Util.queryHistMaxSize);
        }
        else if (queryHist.size()==Util.queryHistMaxSize){
            queryHist.removeFirst();
        }
        queryHist.addLast(query);
    }

    @Override
    public String toString() {
        return "User [username=" + username + ", topicsOfInterest=" + topicsOfInterest + ", queryHist=" + queryHist
                + "]";
    }
    
}
