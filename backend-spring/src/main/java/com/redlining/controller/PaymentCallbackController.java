package com.redlining.controller;

import com.redlining.service.OrderService;
import com.redlining.service.WeChatPayService;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentCallbackController {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackController.class);

    private final WeChatPayService weChatPayService;
    private final OrderService orderService;

    public PaymentCallbackController(WeChatPayService weChatPayService, OrderService orderService) {
        this.weChatPayService = weChatPayService;
        this.orderService = orderService;
    }

    @PostMapping(value = "/wechat/notify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> wechatNotify(
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serialNumber,
            @RequestBody byte[] bodyBytes) {
        if (!weChatPayService.isConfigured()) {
            log.warn("WeChat Pay notify received but WeChat Pay is not configured");
            return ResponseEntity.badRequest().body(Map.of("code", "FAIL", "message", "Not configured"));
        }
        if (signature == null || timestamp == null || nonce == null || serialNumber == null) {
            log.warn("WeChat Pay notify missing required headers");
            return ResponseEntity.badRequest().body(Map.of("code", "FAIL", "message", "Missing headers"));
        }
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        Config config = weChatPayService.getConfig();
        if (config == null) {
            return ResponseEntity.internalServerError().body(Map.of("code", "FAIL", "message", "Config null"));
        }
        try {
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serialNumber)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .body(body)
                    .build();
            NotificationConfig notificationConfig = (config instanceof NotificationConfig) ? (NotificationConfig) config : null;
            if (notificationConfig == null) {
                return ResponseEntity.internalServerError().body(Map.of("code", "FAIL", "message", "Config not NotificationConfig"));
            }
            NotificationParser parser = new NotificationParser(notificationConfig);
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            if (transaction == null) {
                return ResponseEntity.badRequest().body(Map.of("code", "FAIL", "message", "Parse failed"));
            }
            if (!"SUCCESS".equals(transaction.getTradeState())) {
                log.info("WeChat Pay notify trade_state not SUCCESS: {}", transaction.getTradeState());
                return ResponseEntity.ok().body(Map.of("code", "SUCCESS", "message", "OK"));
            }
            String outTradeNo = transaction.getOutTradeNo();
            String transactionId = transaction.getTransactionId();
            Long orderId = parseOrderId(outTradeNo);
            if (orderId == null) {
                log.warn("WeChat Pay notify invalid out_trade_no: {}", outTradeNo);
                return ResponseEntity.badRequest().body(Map.of("code", "FAIL", "message", "Invalid out_trade_no"));
            }
            orderService.completeByPaymentNotification(orderId, transactionId);
            log.info("WeChat Pay order completed: orderId={}, transactionId={}", orderId, transactionId);
            return ResponseEntity.ok().body(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (Exception e) {
            log.warn("WeChat Pay notify error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("code", "FAIL", "message", e.getMessage()));
        }
    }

    private static Long parseOrderId(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(outTradeNo);
        } catch (NumberFormatException e) {
            if (outTradeNo.startsWith("ORD")) {
                try {
                    return Long.parseLong(outTradeNo.substring(3));
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }
    }
}
