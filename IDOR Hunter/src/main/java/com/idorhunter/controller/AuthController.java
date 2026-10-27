package com.idorhunter.controller;

import com.idorhunter.dto.ApiError;
import com.idorhunter.dto.LoginRequest;
import com.idorhunter.dto.UserInfoResponse;
import com.idorhunter.model.AppUser;
import com.idorhunter.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/** Handles session-based login for the SPA frontend and the pentest client. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            AppUser user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow();
            return ResponseEntity.ok(new UserInfoResponse(user.getUsername(), user.getDisplayName(), user.getRole().name()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(new ApiError("INVALID_CREDENTIALS", "Invalid username or password"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(new ApiError("UNAUTHENTICATED", "No active session"));
        }
        AppUser user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(new UserInfoResponse(user.getUsername(), user.getDisplayName(), user.getRole().name()));
    }
}
