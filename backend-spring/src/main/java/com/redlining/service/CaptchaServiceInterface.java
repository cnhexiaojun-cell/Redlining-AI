package com.redlining.service;

import java.util.Map;

public interface CaptchaServiceInterface {

    Map<String, String> createCaptcha();

    boolean validate(String captchaId, String userInput);
}
