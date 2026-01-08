package kz.chitas.chess.model.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "facodes")
@NoArgsConstructor
public class FACode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Instant expiration;

    @OneToOne
    @JoinColumn(name = "fingerprint_id")
    private Fingerprint fingerprint;

    public FACode(String code, Instant expiration, Fingerprint fingerprint) {
        this.code = code;
        this.expiration = expiration;
        this.fingerprint = fingerprint;
    }
}
