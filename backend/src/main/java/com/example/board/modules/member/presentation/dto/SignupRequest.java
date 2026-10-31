package com.example.board.modules.member.presentation.dto;

import com.example.board.modules.member.application.command.SignupCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 120, message = "이메일은 최대 120자까지 가능합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하이어야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하이어야 합니다.")
        String nickname
) {
    public SignupCommand toCommand() {
        return SignupCommand.of(email, password, nickname);
    }
}
