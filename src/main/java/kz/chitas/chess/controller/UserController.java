package kz.chitas.chess.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kz.chitas.chess.model.auth.JWT;
import kz.chitas.chess.model.auth.User;
import kz.chitas.chess.model.auth.DTO.LoginInput;
import kz.chitas.chess.model.auth.DTO.RegisterInput;
import kz.chitas.chess.model.auth.DTO.UserDTO;
import kz.chitas.chess.service.auth.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping
@Log4j2
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody @Valid RegisterInput reg) {
        log.info("Register request for email: {}", reg.getEmail());
        UserDTO registeredUser = userService.register(new User(reg));
        // if (!userService.isVerifiedUser(reg.getEmail(), reg.getFingerprint(), false))
        // {
        // log.info("2FA required for email: {}", reg.getEmail());
        // return ResponseEntity.status(HttpStatus.CONTINUE).body(new UserDTO(0L, "2FA",
        // "The code was sent to this email" + reg.getEmail(), LocalDateTime.now()));
        // }
        log.info("User registered successfully: {}", reg.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<JWT> login(@RequestBody @Valid LoginInput login, HttpServletResponse response) {
        log.info("Login request for username: {}", login.getUsername());

        // if (!userService.isVerifiedUser(login.getUsername(), login.getFingerprint(),
        // true)) {
        // log.info("Email verification required for: {}", login.getUsername());
        // return ResponseEntity.status(HttpStatus.CONTINUE).body(new JWT("VERIFY
        // EMAIL"));
        // }
        JWT token = userService.verify(new User(login), response);
        log.info("Login successful for: {}", login.getUsername());
        return ResponseEntity.ok(token);
    }

    @GetMapping("/refresh")
    public ResponseEntity<JWT> refresh(HttpServletResponse response, HttpServletRequest request) {
        log.info("Token refresh request");
        JWT token = userService.refresh(response, request);
        if (token.getToken().equals("FAILURE")) {
            return ResponseEntity.status(401).body(token);
        }
        ;
        return ResponseEntity.ok(token);
    }

    @GetMapping("/api/search/{query}")
    public List<User> searchUsers(@PathVariable String query) {
        if (query == null || query.isBlank()) {
            return List.of(); // empty list for invalid input
        }
        return userService.searchByUsername(query);
    }
}