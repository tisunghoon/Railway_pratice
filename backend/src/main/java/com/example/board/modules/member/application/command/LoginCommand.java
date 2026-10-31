package com.example.board.modules.member.application.command;

public record LoginCommand(
        String email,
        String password
) {
    public static LoginCommand of(String email, String password) {
        return new LoginCommand(
                email != null ? email.trim().toLowerCase() : "",
                password != null ? password : ""
        );
    }
}
