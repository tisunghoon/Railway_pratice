package com.example.board.modules.member.application;

import com.example.board.modules.member.application.command.LoginCommand;
import com.example.board.modules.member.application.command.SignupCommand;
import com.example.board.modules.member.domain.Member;
import com.example.board.modules.member.domain.MemberRepository;
import com.example.board.shared.error.ApiException;
import com.example.board.shared.security.TokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements MemberFacade {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResult signup(SignupCommand command) {
        if (memberRepository.existsByEmail(command.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        String passwordHash = passwordEncoder.encode(command.password());
        Member member = Member.create(command.email(), passwordHash, command.nickname());
        Member savedMember = memberRepository.save(member);

        String token = tokenProvider.createToken(savedMember.getId());
        return new AuthResult(token, MemberInfo.from(savedMember));
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginCommand command) {
        Member member = memberRepository.findByEmail(command.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(command.password(), member.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = tokenProvider.createToken(member.getId());
        return new AuthResult(token, MemberInfo.from(member));
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInfo getMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        return MemberInfo.from(member);
    }
}
