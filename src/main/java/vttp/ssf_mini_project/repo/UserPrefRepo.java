package vttp.ssf_mini_project.repo;

import java.util.Map;

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

        public void saveUserInfo(String key, String id, String entry){
            stringTemplate.opsForHash().put(key , id, entry);
        }

        public String getUserInfo(String key, String id){
            return (String) stringTemplate.opsForHash().get(key, id);
        }

        public boolean userExists(String id){
            return stringTemplate.opsForHash().hasKey(Util.interests, id);
        }

        public Map<Object, Object> getPrefEntries(){
            return stringTemplate.opsForHash().entries(Util.interests);
        }
}
