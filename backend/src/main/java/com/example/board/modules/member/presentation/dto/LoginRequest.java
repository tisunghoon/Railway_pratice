package com.example.board.modules.member.presentation.dto;

import com.example.board.modules.member.application.command.LoginCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 120, message = "이메일은 최대 120자까지 가능합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        String password
) {
    public LoginCommand toCommand() {
        return LoginCommand.of(email, password);
    }
}
