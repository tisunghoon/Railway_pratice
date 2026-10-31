package com.example.board.modules.member.application;

import com.example.board.modules.member.domain.Member;

public record MemberInfo(
        Long id,
        String email,
        String nickname
) {
    public static MemberInfo from(Member member) {
        return new MemberInfo(member.getId(), member.getEmail(), member.getNickname());
    }
}
