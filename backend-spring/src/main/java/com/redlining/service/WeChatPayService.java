package com.redlining.service;

import com.redlining.config.AppProperties;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.util.PemUtil;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;

@Service
public class WeChatPayService {

    private static final Logger log = LoggerFactory.getLogger(WeChatPayService.class);

    private final AppProperties appProperties;
    private volatile Config config;
    private volatile NativePayService nativePayService;

    public WeChatPayService(AppProperties appProperties) {
        this.appProperties = appProperties;
        initIfConfigured();
    }

    private static PrivateKey loadPrivateKey(AppProperties.Wechatpay w) throws Exception {
        String raw = w.getMerchantPrivateKey();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("merchant-private-key is empty");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("file:")) {
            String pathStr = trimmed.substring(5).trim();
            Path path = Paths.get(pathStr);
            if (!Files.isReadable(path)) {
                throw new IllegalArgumentException("merchant-private-key file not readable: " + pathStr);
            }
            return PemUtil.loadPrivateKeyFromPath(path.toString());
        }
        if (trimmed.startsWith("-----BEGIN")) {
            return PemUtil.loadPrivateKeyFromString(trimmed);
        }
        throw new IllegalArgumentException(
                "merchant-private-key must be PEM content (-----BEGIN PRIVATE KEY-----...) or file path (file:/path/to/apiclient_key.pem). "
                        + "Get PEM from WeChat Merchant Platform -> API Security -> API Certificate (apiclient_key.pem).");
    }

    private void initIfConfigured() {
        if (!appProperties.getWechatpay().isConfigured()) {
            return;
        }
        try {
            AppProperties.Wechatpay w = appProperties.getWechatpay();
            PrivateKey privateKey = loadPrivateKey(w);
            config = new RSAAutoCertificateConfig.Builder()
                    .merchantId(w.getMchId())
                    .privateKey(privateKey)
                    .merchantSerialNumber(w.getMerchantSerialNo())
                    .apiV3Key(w.getApiV3Key())
                    .build();
            nativePayService = new NativePayService.Builder().config(config).build();
            log.info("WeChat Pay initialized for mch {}", w.getMchId());
        } catch (Exception e) {
            log.warn("WeChat Pay init failed: {}", e.getMessage());
            config = null;
            nativePayService = null;
        }
    }

    /** @return code_url for QR code, or null if not configured or request failed */
    public String createNativeOrder(String outTradeNo, int totalCents, String description) {
        if (nativePayService == null) {
            return null;
        }
        AppProperties.Wechatpay w = appProperties.getWechatpay();
        if (totalCents <= 0) {
            log.warn("WeChat Pay: total must be positive");
            return null;
        }
        try {
            PrepayRequest request = new PrepayRequest();
            request.setAppid(w.getAppId());
            request.setMchid(w.getMchId());
            request.setDescription(description);
            request.setOutTradeNo(outTradeNo);
            request.setNotifyUrl(w.getNotifyUrl());
            Amount amount = new Amount();
            amount.setTotal(totalCents);
            amount.setCurrency("CNY");
            request.setAmount(amount);

            PrepayResponse response = nativePayService.prepay(request);
            String codeUrl = response.getCodeUrl();
            if (codeUrl != null && !codeUrl.isEmpty()) {
                log.info("WeChat Pay native order created: outTradeNo={}", outTradeNo);
                return codeUrl;
            }
        } catch (Exception e) {
            log.warn("WeChat Pay native prepay failed: {}", e.getMessage());
        }
        return null;
    }

    public boolean isConfigured() {
        return nativePayService != null;
    }

    public Config getConfig() {
        return config;
    }
}
