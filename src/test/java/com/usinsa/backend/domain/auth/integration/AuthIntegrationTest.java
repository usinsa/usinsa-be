package com.usinsa.backend.domain.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usinsa.backend.domain.auth.dto.AuthDto;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.search.port.ProductIndexPort;
import com.usinsa.backend.domain.search.port.ProductSearchPort;
import com.usinsa.backend.domain.search.port.ProductVectorSearchPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Auth 통합 테스트
 * - 실제 Spring Context 로드
 * - 전체 인증 플로우 검증 (로그인 → 인증 요청 → 토큰 갱신 → 로그아웃)
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration")
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("local")
@DisplayName("Auth 통합 테스트")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ProductIndexPort productIndexPort;

    @MockBean
    private ProductSearchPort productSearchPort;

    @MockBean
    private ProductVectorSearchPort productVectorSearchPort;

    private Member testMember;
    private String testPassword = "password123!";

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = Member.builder()
                .usinaId("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode(testPassword))
                .name("테스트유저")
                .nickname("테스터")
                .phone("01012345678")
                .isAdmin(false)
                .build();
        
        memberRepository.save(testMember);
    }

    @Test
    @DisplayName("전체 인증 플로우 - 로그인 → 인증 요청 → 토큰 갱신 → 로그아웃")
    void fullAuthFlow() throws Exception {
        // 1. 로그인
        AuthDto.LoginReq loginReq = new AuthDto.LoginReq();
        loginReq.setEmail(testMember.getEmail());
        loginReq.setPassword(testPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.memberId").value(testMember.getId()))
                .andExpect(jsonPath("$.data.email").value(testMember.getEmail()))
                .andReturn();

        String loginResponseJson = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponseJson)
                .path("data").path("accessToken").asText();

        assertThat(accessToken).isNotBlank();

        // 로그인 시 내려온 쿠키 (accessToken/refreshToken/deviceId) 확보
        jakarta.servlet.http.Cookie[] loginCookies = loginResult.getResponse().getCookies();
        assertThat(loginCookies).isNotEmpty();

        // 2. 인증이 필요한 요청 (Access Token 사용)
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testMember.getEmail()));

        // 3. 토큰 갱신 (Refresh Token 쿠키 사용)
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(loginCookies))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        String refreshResponseJson = refreshResult.getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(refreshResponseJson)
                .path("data").path("accessToken").asText();

        assertThat(newAccessToken).isNotBlank();
        assertThat(newAccessToken).isNotEqualTo(accessToken);  // 새 토큰 발급 확인

        // 4. 새 Access Token으로 인증 요청
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andDo(print())
                .andExpect(status().isOk());

        // 5. 로그아웃
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 6. 로그아웃 후 토큰 사용 시도 (실패해야 함)
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_WrongPassword() throws Exception {
        AuthDto.LoginReq loginReq = new AuthDto.LoginReq();
        loginReq.setEmail(testMember.getEmail());
        loginReq.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_MemberNotFound() throws Exception {
        AuthDto.LoginReq loginReq = new AuthDto.LoginReq();
        loginReq.setEmail("notfound@example.com");
        loginReq.setPassword(testPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("인증 없이 /me 호출 - Access Token 없어 401 대신 잘못된 요청으로 처리된다")
    void auth_Fail_NoAuthHeader() throws Exception {
        // /api/v1/auth/me 는 SecurityConfig 상 permitAll 이므로
        // 인증 정보가 없으면 필터가 principal 을 세팅하지 않아 memberId=null 로 서비스에 전달되고
        // Repository 단에서 IllegalArgumentException → 400(BAD_REQUEST) 로 응답한다.
        mockMvc.perform(get("/api/v1/auth/me"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("인증 실패 - 잘못된 토큰으로 /me 호출 시에도 인증 정보가 세팅되지 않는다")
    void auth_Fail_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Refresh Token 쿠키 없음")
    void refresh_Fail_NoCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("JWT_007"));
    }

    @Test
    @DisplayName("Refresh Token 재사용 공격 시나리오")
    void refreshToken_ReuseAttack() throws Exception {
        // 1. 로그인
        AuthDto.LoginReq loginReq = new AuthDto.LoginReq();
        loginReq.setEmail(testMember.getEmail());
        loginReq.setPassword(testPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        jakarta.servlet.http.Cookie[] loginCookies = loginResult.getResponse().getCookies();

        // 2. 첫 번째 갱신 (정상)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(loginCookies))
                .andDo(print())
                .andExpect(status().isOk());

        // 3. 같은(로그인 시 발급된) Refresh Token 쿠키로 재시도 (공격 시나리오)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(loginCookies))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("JWT_004"));
    }
}
