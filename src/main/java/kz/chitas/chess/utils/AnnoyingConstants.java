package kz.chitas.chess.utils;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import kz.chitas.chess.model.auth.User;
import kz.chitas.chess.repo.UsersRepo;

@Component
public class AnnoyingConstants {
    private final UsersRepo usersRepo;

    public AnnoyingConstants(UsersRepo usersRepo) {
        this.usersRepo = usersRepo;

    }

    public User getCurrentUser() {
        return usersRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get();
    }

    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

}
