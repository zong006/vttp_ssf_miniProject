package vttp.ssf_mini_project.model;

import java.util.List;

import jakarta.validation.constraints.Size;

public class User {

    @Size(min = 5, max = 30, message = "Username ust be between 5 to 30 characters in length.")
    private String username;

    @Size(min = 1, message = "Please select at least one topic of interest.")
    private List<String> topicsOfInterest;

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
    @Override
    public String toString() {
        return "User [username=" + username + ", topicsOfInterest=" + topicsOfInterest + "]";
    }
    
}
