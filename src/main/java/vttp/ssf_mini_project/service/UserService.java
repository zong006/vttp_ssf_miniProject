package vttp.ssf_mini_project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import vttp.ssf_mini_project.repo.UserPrefRepo;
import vttp.ssf_mini_project.util.Util;

@Service
public class UserService {

    @Autowired
    UserPrefRepo userPrefRepo;

    public boolean userExists(String username){
        return userPrefRepo.userExists(username + Util.delimiter + "userPref");
    }

    public void saveUserPref(String username, List<String> topicsOfInterest){
        userPrefRepo.saveUserPref(username + Util.delimiter + "userPref", topicsOfInterest);
    }

    public List<String> getUserPref(String username){
        return userPrefRepo.getUserPref(username + Util.delimiter + "userPref");
    }
}


