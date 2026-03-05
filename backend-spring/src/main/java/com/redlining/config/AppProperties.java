package com.redlining.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Llm llm = new Llm();
    private final File file = new File();
    private final Cache cache = new Cache();
    private final Minio minio = new Minio();
    private final OnlyOffice onlyOffice = new OnlyOffice();
    private final Wechatpay wechatpay = new Wechatpay();

    public Jwt getJwt() {
        return jwt;
    }

    public Llm getLlm() {
        return llm;
    }

    public File getFile() {
        return file;
    }

    public Cache getCache() {
        return cache;
    }

    public Minio getMinio() {
        return minio;
    }

    public OnlyOffice getOnlyOffice() {
        return onlyOffice;
    }

    public Wechatpay getWechatpay() {
        return wechatpay;
    }

    public static class Wechatpay {
        private String appId = "";
        private String mchId = "";
        private String apiV3Key = "";
        private String merchantPrivateKey = "";
        private String merchantSerialNo = "";
        private String notifyUrl = "";

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId != null ? appId : ""; }
        public String getMchId() { return mchId; }
        public void setMchId(String mchId) { this.mchId = mchId != null ? mchId : ""; }
        public String getApiV3Key() { return apiV3Key; }
        public void setApiV3Key(String apiV3Key) { this.apiV3Key = apiV3Key != null ? apiV3Key : ""; }
        public String getMerchantPrivateKey() { return merchantPrivateKey; }
        public void setMerchantPrivateKey(String merchantPrivateKey) { this.merchantPrivateKey = merchantPrivateKey != null ? merchantPrivateKey : ""; }
        public String getMerchantSerialNo() { return merchantSerialNo; }
        public void setMerchantSerialNo(String merchantSerialNo) { this.merchantSerialNo = merchantSerialNo != null ? merchantSerialNo : ""; }
        public String getNotifyUrl() { return notifyUrl; }
        public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl != null ? notifyUrl : ""; }

        public boolean isConfigured() {
            return appId != null && !appId.isEmpty()
                    && mchId != null && !mchId.isEmpty()
                    && apiV3Key != null && !apiV3Key.isEmpty()
                    && merchantPrivateKey != null && !merchantPrivateKey.isEmpty()
                    && merchantSerialNo != null && !merchantSerialNo.isEmpty()
                    && notifyUrl != null && !notifyUrl.isEmpty();
        }
    }

    public static class OnlyOffice {
        private String documentServerUrl = "http://127.0.0.1:8080";
        private String apiBaseUrl = "http://localhost:8003";
        private String jwtSecret = "";
        private boolean jwtEnabled = false;

        public String getDocumentServerUrl() {
            return documentServerUrl;
        }

        public void setDocumentServerUrl(String documentServerUrl) {
            this.documentServerUrl = documentServerUrl;
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public boolean isJwtEnabled() {
            return jwtEnabled;
        }

        public void setJwtEnabled(boolean jwtEnabled) {
            this.jwtEnabled = jwtEnabled;
        }
    }

    public static class Minio {
        private String endpoint = "http://127.0.0.1:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "avatars";
        private String documentsBucket = "documents";
        private final Avatar avatar = new Avatar();

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getDocumentsBucket() { return documentsBucket; }
        public void setDocumentsBucket(String documentsBucket) { this.documentsBucket = documentsBucket; }
        public Avatar getAvatar() { return avatar; }

        public static class Avatar {
            private long maxSize = 5 * 1024 * 1024L;
            private String allowedContentTypes = "image/jpeg,image/png";

            public long getMaxSize() { return maxSize; }
            public void setMaxSize(long maxSize) { this.maxSize = maxSize; }
            public String getAllowedContentTypes() { return allowedContentTypes; }
            public void setAllowedContentTypes(String allowedContentTypes) { this.allowedContentTypes = allowedContentTypes; }
            public String[] getAllowedContentTypesArray() {
                return allowedContentTypes == null ? new String[0] : allowedContentTypes.split(",");
            }
        }
    }

    public static class Jwt {
        private String secret = "change-me-in-production";
        private int expirationMinutes = 60;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(int expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }

    public static class Llm {
        /** 当前使用的 LLM：deepseek | zhipu | modelscope */
        private String provider = "deepseek";
        private String deepseekApiKey = "";
        private String deepseekBaseUrl = "https://api.deepseek.com";
        private String zhipuApiKey = "";
        private String zhipuBaseUrl = "https://open.bigmodel.cn/api/paas/v4";
        private String zhipuModelName = "glm-4-flash";
        private String modelscopeApiKey = "your_api_key_here";
        private String modelscopeBaseUrl = "https://api-inference.modelscope.cn/v1";
        private String modelName = "deepseek-chat";

        public String getApiKey() {
            if ("zhipu".equalsIgnoreCase(provider) && zhipuApiKey != null && !zhipuApiKey.isEmpty()) {
                return zhipuApiKey;
            }
            if ("deepseek".equalsIgnoreCase(provider) && deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
                return deepseekApiKey;
            }
            if ("modelscope".equalsIgnoreCase(provider) && modelscopeApiKey != null && !modelscopeApiKey.isEmpty()) {
                return modelscopeApiKey;
            }
            return (deepseekApiKey != null && !deepseekApiKey.isEmpty())
                    ? deepseekApiKey
                    : (zhipuApiKey != null && !zhipuApiKey.isEmpty())
                    ? zhipuApiKey
                    : modelscopeApiKey;
        }

        public String getBaseUrl() {
            if ("zhipu".equalsIgnoreCase(provider) && zhipuApiKey != null && !zhipuApiKey.isEmpty()) {
                return zhipuBaseUrl;
            }
            if ("deepseek".equalsIgnoreCase(provider) && deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
                return deepseekBaseUrl;
            }
            if ("modelscope".equalsIgnoreCase(provider) && modelscopeApiKey != null && !modelscopeApiKey.isEmpty()) {
                return modelscopeBaseUrl;
            }
            if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) return deepseekBaseUrl;
            if (zhipuApiKey != null && !zhipuApiKey.isEmpty()) return zhipuBaseUrl;
            return modelscopeBaseUrl;
        }

        public String getModelName() {
            if ("zhipu".equalsIgnoreCase(provider) && zhipuApiKey != null && !zhipuApiKey.isEmpty()) {
                return zhipuModelName != null && !zhipuModelName.isEmpty() ? zhipuModelName : "glm-4-flash";
            }
            return modelName;
        }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getDeepseekApiKey() { return deepseekApiKey; }
        public void setDeepseekApiKey(String deepseekApiKey) { this.deepseekApiKey = deepseekApiKey; }
        public String getDeepseekBaseUrl() { return deepseekBaseUrl; }
        public void setDeepseekBaseUrl(String deepseekBaseUrl) { this.deepseekBaseUrl = deepseekBaseUrl; }
        public String getZhipuApiKey() { return zhipuApiKey; }
        public void setZhipuApiKey(String zhipuApiKey) { this.zhipuApiKey = zhipuApiKey; }
        public String getZhipuBaseUrl() { return zhipuBaseUrl; }
        public void setZhipuBaseUrl(String zhipuBaseUrl) { this.zhipuBaseUrl = zhipuBaseUrl; }
        public String getZhipuModelName() { return zhipuModelName; }
        public void setZhipuModelName(String zhipuModelName) { this.zhipuModelName = zhipuModelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public String getModelscopeApiKey() { return modelscopeApiKey; }
        public void setModelscopeApiKey(String modelscopeApiKey) { this.modelscopeApiKey = modelscopeApiKey; }
        public String getModelscopeBaseUrl() { return modelscopeBaseUrl; }
        public void setModelscopeBaseUrl(String modelscopeBaseUrl) { this.modelscopeBaseUrl = modelscopeBaseUrl; }
    }

    public static class File {
        private long maxSize = 10 * 1024 * 1024L;
        private String allowedExtensions = ".pdf,.docx,.txt";

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

        public String[] getAllowedExtensionsArray() {
            return allowedExtensions == null ? new String[0] : allowedExtensions.split(",");
        }

        public void setAllowedExtensions(String allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }
    }

    public static class Cache {
        private long analysisTtlSeconds = 3600;

        public long getAnalysisTtlSeconds() {
            return analysisTtlSeconds;
        }

        public void setAnalysisTtlSeconds(long analysisTtlSeconds) {
            this.analysisTtlSeconds = analysisTtlSeconds;
        }
    }
}
