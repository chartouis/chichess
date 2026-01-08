package kz.chitas.chess.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.chitas.chess.model.auth.JWT;
import kz.chitas.chess.model.auth.User;
import kz.chitas.chess.model.auth.DTO.UserDTO;
import kz.chitas.chess.repo.UsersRepo;
import lombok.extern.log4j.Log4j2;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Log4j2
public class UserService {
    private final UsersRepo repo;
    private final AuthenticationManager manager;
    private final JWTService jwtService;
    private final CookieService cook;
    private final int REFRESH_TOKEN_AGE = 60 * 60 * 24 * 30;
    private final int ACCESS_TOKEN_AGE = 60 * 10;

    public UserService(UsersRepo repo, AuthenticationManager manager, JWTService jwtservice, CookieService cook) {
        this.repo = repo;
        this.manager = manager;
        this.jwtService = jwtservice;
        this.cook = cook;
    }

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public UserDTO register(User user) {
        if (user.getPassword() == null || !isValidEmail(user.getEmail())) {
            log.warn("Invalid registration attempt: null password or invalid email {}", user.getEmail());
            return null;
        }
        if (user.getUsername() == null) {
            user.setUsername(user.getEmail().split("@")[0]);
        }
        if (repo.existsByUsername(user.getUsername())) {
            log.warn("Username {} already exists", user.getUsername());
            return getDefaultUserDTO();
        }
        if (repo.existsByEmail(user.getEmail())) {
            log.warn("Email {} already exists", user.getEmail());
            return getDefaultUserDTO();
        }
        user.setPassword(encoder.encode(user.getPassword()));
        repo.save(user);
        log.info("User registered: {}", user.getUsername());
        return new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }

    public JWT verify(User user, HttpServletResponse response) {
        log.info("Verifying user: {}", user.getUsername());
        Authentication authentication = manager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        if (authentication.isAuthenticated()) {
            String tok = jwtService.generateToken(user.getUsername());
            cook.setCookie(tok, response, "REFRESH-TOKEN-JWTAUTH", "/refresh", REFRESH_TOKEN_AGE);
            log.info("User {} authenticated successfully", user.getUsername());
            return new JWT("SUCCESS");
        }
        log.warn("Authentication failed for user: {}", user.getUsername());
        return new JWT("FAILURE");
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String emailRegex = "^(?!\\.)[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-z]{2,}$";

        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public UserDTO userToDTO(User user) {
        return new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }

    private UserDTO getDefaultUserDTO() {
        String cause = "Invalid username or password.";
        return new UserDTO(0L, "Failed to register", cause, LocalDateTime.MIN);
    }

    public JWT refresh(HttpServletResponse response, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Refreshing token for user: {}", username);
        String tok = jwtService.generateToken(username);
        if (tok == null || !jwtService.validateToken(cook.getToken(request, false))) {
            log.warn("Token generation failed for user: {}", username);
            return new JWT("FAILURE");
        }
        cook.setCookie(tok, response, "ACCESS-TOKEN-JWTAUTH", "/api", ACCESS_TOKEN_AGE);
        log.info("Token refreshed for user: {}", username);
        return new JWT("SUCCESS");
    }
}