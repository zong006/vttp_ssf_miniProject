package vttp.ssf_mini_project.repo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import vttp.ssf_mini_project.util.Util;

@Repository
public class UserPrefRepo {
        @Autowired
        @Qualifier(Util.template)
        RedisTemplate<String, String> stringTemplate;

        public void saveUserPref(String id, List<String> entry){
            stringTemplate.opsForList().leftPushAll(id, entry);
        }

        public List<String> getUserPref(String id){
            Long lengthOfList = stringTemplate.opsForList().size(id);
            return stringTemplate.opsForList().range(id, 0, lengthOfList);
        }

        public boolean userExists(String id){
            return stringTemplate.hasKey(id);
        }

        // add method to modify user pref
}
