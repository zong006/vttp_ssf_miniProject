package vttp.ssf_mini_project.controller;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import vttp.ssf_mini_project.model.User;
import vttp.ssf_mini_project.service.ArticleService;
import vttp.ssf_mini_project.service.UserService;

@Controller
public class UserController {
    
    @Autowired
    UserService userService;

    @Autowired
    ArticleService articleService;

    @GetMapping("/")
    public String landingPage(){
        return "landingPage";
    }

    @GetMapping("/login")
    public String loginPage(Model model){
        model.addAttribute("user", new User());
        return "loginPage";
    }

    @PostMapping("/login")
    public String verifyLogin(@Valid @ModelAttribute User user, BindingResult bindingResult, HttpSession httpSession, Model model) throws JsonMappingException, JsonProcessingException{

        if (bindingResult.hasErrors()){
            return "loginPage";
        }
        if (userService.userExists(user.getUsername())){
            List<String> topicsOfInterest = userService.getUserPref(user.getUsername());
            user.setTopicsOfInterest(topicsOfInterest);
            Deque<String> queries = userService.getQueryEntries(user.getUsername());
            user.setQueryHist(queries);
            // need to set user query deque
            System.out.println(user.toString()); // delete this later
            httpSession.setAttribute("user", user);
            
            return "redirect:/latest";
        }
        model.addAttribute("errorMessageUser", "The username you entered does not exist. Please create a new account.");
        return "errorPage";
    }

    @GetMapping("/create")
    public String newUserPage(HttpSession httpSession, Model model){
        
        model.addAttribute("user", new User());
        Map<String, String> sectionMap = articleService.getSections();

        httpSession.setAttribute("sectionMap", sectionMap);
        
        return "newUserPage";
    }

    @PostMapping("/create")
    public String postLoginDetails(@Valid @ModelAttribute User user, BindingResult bindingResult, Model model, HttpSession httpSession) throws JsonProcessingException{
        
        
        if (bindingResult.hasErrors()){
            // model.addAttribute("sectionMap", articleService.getSections());
            return "newUserPage";
        }

        httpSession.setAttribute("user", user);
        User u = (User) httpSession.getAttribute("user"); //
        System.out.println(u.getUsername()); //
        // System.out.println(httpSession.getAttribute(user.getUsername()));
        
        if (userService.userExists(user.getUsername())){
            System.out.println("user exsits"); // delete this later
            model.addAttribute("errorMessageUser", "Username already exists. Please choose another username.");
            return "errorPage";
        }
        // a new user. pass the topics of interest as a list to the service to be processed
        System.out.println("a new user"); // delete this later
        userService.updateUserPref(user.getUsername(), user.getTopicsOfInterest());

        return "redirect:/latest";
    }

    @PostMapping("/logout")
    public String logout(HttpSession httpSession){
        httpSession.removeAttribute("user");
        httpSession.invalidate();

        return "redirect:/";
    }

}
