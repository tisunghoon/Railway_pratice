package com.example.board.modules.member.application;

public record AuthResult(
        String accessToken,
        MemberInfo member
) {
}
