package com.example.board.modules.member.presentation.dto;

import com.example.board.modules.member.application.AuthResult;
import com.example.board.modules.member.application.MemberInfo;

public record AuthResponse(
        String accessToken,
        MemberInfo member
) {
    public static AuthResponse from(AuthResult authResult) {
        return new AuthResponse(authResult.accessToken(), authResult.member());
    }
}
