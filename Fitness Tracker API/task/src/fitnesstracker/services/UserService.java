package fitnesstracker.services;

import fitnesstracker.models.User;
import fitnesstracker.models.UserDetailsAdapter;
import fitnesstracker.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User " + username + " not found");
        }
        User developer = userOptional.get();
        org.springframework.security.core.userdetails.User newUser = new org.springframework.security.core.userdetails.User(
                developer.getEmail(),
                developer.getPassword(),
                Collections.emptyList()
        );
        System.out.println("Loaded user:\n" + newUser);
        return newUser;
        //return new UserDetailsAdapter(developer);
    }
}
