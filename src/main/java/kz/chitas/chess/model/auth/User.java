package kz.chitas.chess.model.auth;

import jakarta.persistence.*;
import kz.chitas.chess.model.auth.DTO.LoginInput;
import kz.chitas.chess.model.auth.DTO.RegisterInput;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private int rating = 1200;

    @CreatedDate
    private LocalDateTime createdAt;

    public User(RegisterInput reg) {
        this.email = reg.getEmail();
        this.username = reg.getUsername();
        this.password = reg.getPassword();
    }

    public User(LoginInput login) {
        this.username = login.getUsername();
        this.password = login.getPassword();
    }

}
