package com.redlining.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redlining.dto.LoginRequest;
import com.redlining.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    /** test 环境下验证码固定为 TEST，见 TestCaptchaConfig */
    private static final String CAPTCHA_CODE = "TEST";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCaptcha_returnsOkWithCaptchaIdAndImage() throws Exception {
        mockMvc.perform(get("/api/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaId").isString())
                .andExpect(jsonPath("$.image").value("data:image/png;base64,test"))
                .andReturn();
    }

    @Test
    void register_thenLogin_thenMe_fullFlow() throws Exception {
        // 1. 获取验证码（test 环境固定 code=TEST）
        String captchaBody = mockMvc.perform(get("/api/captcha"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String captchaId = objectMapper.readTree(captchaBody).get("captchaId").asText();

        String username = "testuser_" + System.currentTimeMillis();
        String password = "pass123";

        // 2. Register
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername(username);
        registerReq.setPassword(password);
        registerReq.setCaptchaId(captchaId);
        registerReq.setCaptchaCode(CAPTCHA_CODE);

        MvcResult registerResult = mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").isString())
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.id").isNumber())
                .andReturn();

        String registerBody = registerResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(registerBody).get("access_token").asText();

        // 3. Login（新验证码，test 环境仍用 code=TEST）
        String loginCaptchaBody = mockMvc.perform(get("/api/captcha")).andReturn().getResponse().getContentAsString();
        String loginCaptchaId = objectMapper.readTree(loginCaptchaBody).get("captchaId").asText();
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(username);
        loginReq.setPassword(password);
        loginReq.setCaptchaId(loginCaptchaId);
        loginReq.setCaptchaCode(CAPTCHA_CODE);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isString())
                .andExpect(jsonPath("$.user.username").value(username));

        // 4. GET /api/me with token
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void register_invalidCaptcha_returns400() throws Exception {
        String captchaId = objectMapper.readTree(
                mockMvc.perform(get("/api/captcha")).andReturn().getResponse().getContentAsString()
        ).get("captchaId").asText();

        RegisterRequest req = new RegisterRequest();
        req.setUsername("someone");
        req.setPassword("pass123");
        req.setCaptchaId(captchaId);
        req.setCaptchaCode("wrong");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_invalidCaptcha_returns400() throws Exception {
        String captchaId = objectMapper.readTree(
                mockMvc.perform(get("/api/captcha")).andReturn().getResponse().getContentAsString()
        ).get("captchaId").asText();

        LoginRequest req = new LoginRequest();
        req.setUsername("nobody");
        req.setPassword("pass123");
        req.setCaptchaId(captchaId);
        req.setCaptchaCode("wrong");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateUsername_returns400() throws Exception {
        String captchaBody1 = mockMvc.perform(get("/api/captcha")).andReturn().getResponse().getContentAsString();
        String captchaId1 = objectMapper.readTree(captchaBody1).get("captchaId").asText();

        String username = "dupuser_" + System.currentTimeMillis();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword("pass123");
        req.setCaptchaId(captchaId1);
        req.setCaptchaCode(CAPTCHA_CODE);

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        String captchaBody2 = mockMvc.perform(get("/api/captcha")).andReturn().getResponse().getContentAsString();
        req.setCaptchaId(objectMapper.readTree(captchaBody2).get("captchaId").asText());

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
