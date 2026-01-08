package kz.chitas.chess.service.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class CookieService {

    public String setCookie(String jwtToken, HttpServletResponse response, String name, String path, int age) {
        log.info("Setting cookie: {} with path: {} and maxAge: {}", name, path, age);
        ResponseCookie cookie = ResponseCookie.from(name, jwtToken)
                .path(path)
                .httpOnly(true)
                .secure(false)
                // .sameSite("None") uncomment on deploy
                .maxAge(age)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.debug("Cookie set: {}", cookie.toString());
        return cookie.toString();
    }

    public String getToken(HttpServletRequest request, boolean isAccessToken) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.warn("No cookies found in request");
            return null;
        }

        for (Cookie ck : cookies) {
            String name = ck.getName();
            if (isAccessToken && "ACCESS-TOKEN-JWTAUTH".equals(name)) {
                log.info("Found token in cookie: {}", name);
                return ck.getValue();
            }
            if (!isAccessToken && "REFRESH-TOKEN-JWTAUTH".equals(name)) {
                log.info("Found token in cookie: {}", name);
                return ck.getValue();
            }
        }
        log.warn("No matching token found in cookies");
        return null;
    }
}