package com.example.board.modules.member.application.command;

public record SignupCommand(
        String email,
        String password,
        String nickname
) {
    public static SignupCommand of(String email, String password, String nickname) {
        return new SignupCommand(
                email != null ? email.trim().toLowerCase() : "",
                password != null ? password : "",
                nickname != null ? nickname.trim() : ""
        );
    }
}
