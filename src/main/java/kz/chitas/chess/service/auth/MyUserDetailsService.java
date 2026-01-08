package kz.chitas.chess.service.auth;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import kz.chitas.chess.model.auth.User;
import kz.chitas.chess.model.auth.UserPrincipal;
import kz.chitas.chess.repo.UsersRepo;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UsersRepo repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = repo.findByUsername(username);
        if (!user.isPresent()) {
            log.warn("User was not found: {}", username);
            throw new UsernameNotFoundException(username + " user was not found");
        }
        return new UserPrincipal(user.orElseThrow());
    }

}
