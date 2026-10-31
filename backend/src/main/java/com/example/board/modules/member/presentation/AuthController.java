package com.example.board.modules.member.presentation;

import com.example.board.modules.member.application.AuthResult;
import com.example.board.modules.member.application.AuthService;
import com.example.board.modules.member.application.MemberInfo;
import com.example.board.modules.member.presentation.dto.AuthResponse;
import com.example.board.modules.member.presentation.dto.LoginRequest;
import com.example.board.modules.member.presentation.dto.SignupRequest;
import com.example.board.shared.security.TokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenProvider tokenProvider;

    public AuthController(AuthService authService, TokenProvider tokenProvider) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResult result = authService.signup(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.toCommand());
        return ResponseEntity.ok(AuthResponse.from(result));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberInfo> getMe(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        Long memberId = tokenProvider.resolveMemberId(authHeader);
        MemberInfo memberInfo = authService.getMemberById(memberId);
        return ResponseEntity.ok(memberInfo);
    }
}
