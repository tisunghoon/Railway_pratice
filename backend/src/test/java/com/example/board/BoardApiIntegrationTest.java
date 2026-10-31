package com.example.board;

import com.example.board.modules.member.domain.MemberRepository;
import com.example.board.modules.member.presentation.dto.LoginRequest;
import com.example.board.modules.member.presentation.dto.SignupRequest;
import com.example.board.modules.post.domain.PostRepository;
import com.example.board.modules.post.presentation.dto.PostCreateRequest;
import com.example.board.shared.security.TokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BoardApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("동일 이메일 2건 동시 가입 시 1건은 201, 1건은 409 Conflict가 발생하고 DB에는 1건만 저장된다")
    void concurrentSignupHandlesDuplicateSafely() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        SignupRequest request = new SignupRequest("concurrent@example.com", "password1234", "동시러너");
        String requestJson = objectMapper.writeValueAsString(request);

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andReturn();
                return result.getResponse().getStatus();
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : futures) {
            statuses.add(f.get());
        }
        executor.shutdown();

        Collections.sort(statuses);
        assertThat(statuses).containsExactly(201, 409);
        assertThat(memberRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("회원가입 -> 로그인 -> 내 정보 조회 정상 흐름")
    void authNormalFlow() throws Exception {
        SignupRequest signup = new SignupRequest("user1@example.com", "password1234", "유저1");
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.member.email").value("user1@example.com"))
                .andReturn();

        String token = objectMapper.readTree(signupResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        LoginRequest login = new LoginRequest("user1@example.com", "password1234");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("유저1"));
    }

    @Test
    @DisplayName("인증 헤더 누락 및 변조된 토큰으로 요청 시 401 Unauthorized가 반환된다")
    void authHeaderFailures() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증 헤더가 누락되었습니다."));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer 1.9999999999.invalidSignature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증 토큰 서명 검증에 실패했습니다."));

        String expiredToken = "1.1000000000.dummy";
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("페이징 음수 입력 시 400 Bad Request 에러를 반환한다")
    void pagingValidationFails() throws Exception {
        mockMvc.perform(get("/api/posts?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("페이지 번호는 0 이상이어야 합니다."));

        mockMvc.perform(get("/api/posts?size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다."));
    }

    @Test
    @DisplayName("타입 불일치 및 잘못된 JSON 형식 요청 시 500이 아닌 400 Bad Request가 반환된다")
    void badRequestHandlingMvcExceptions() throws Exception {
        mockMvc.perform(get("/api/posts?page=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 파라미터 또는 경로 변수 타입이 올바르지 않습니다."));

        mockMvc.perform(get("/api/posts/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 파라미터 또는 경로 변수 타입이 올바르지 않습니다."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 본문 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("표준 HTTP 오류: 405 Method Not Allowed, 415 Unsupported Media Type, 404 No Resource Found가 정상 반환된다")
    void standardHttpErrorsArePreserved() throws Exception {
        mockMvc.perform(put("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("지원하지 않는 HTTP 메서드입니다."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("지원하지 않는 미디어 타입입니다."));

        mockMvc.perform(get("/api/no-such-route-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("게시글 작성, 페이징 목록 조회(authorId 포함/content 제외), 상세 조회, 본인 및 타인 삭제 검증")
    void postCrudAndOwnershipWorkflow() throws Exception {
        SignupRequest userA = new SignupRequest("authorA@example.com", "password1234", "작성자A");
        String tokenA = extractToken(mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userA))).andReturn());

        SignupRequest userB = new SignupRequest("authorB@example.com", "password1234", "작성자B");
        String tokenB = extractToken(mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userB))).andReturn());

        PostCreateRequest createReq = new PostCreateRequest("테스트 제목입니다", "본문 상세 내용입니다.");
        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.authorNickname").value("작성자A"))
                .andReturn();

        long postId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult listResult = mockMvc.perform(get("/api/posts?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].id").value(postId))
                .andExpect(jsonPath("$.items[0].authorId").isNumber())
                .andExpect(jsonPath("$.items[0].authorNickname").value("작성자A"))
                .andExpect(jsonPath("$.items[0].title").value("테스트 제목입니다"))
                .andReturn();

        JsonNode firstItem = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .get("items").get(0);
        assertThat(firstItem.has("content")).isFalse();

        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("본문 상세 내용입니다."));

        mockMvc.perform(delete("/api/posts/" + postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("본인이 작성한 게시글만 삭제할 수 있습니다."));

        mockMvc.perform(delete("/api/posts/" + postId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
    }

    private String extractToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
