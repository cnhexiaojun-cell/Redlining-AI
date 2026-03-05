package com.redlining.service;

import com.redlining.config.AppProperties;
import com.redlining.dto.LoginResponse;
import com.redlining.dto.RegisterRequest;
import com.redlining.dto.UpdateProfileRequest;
import com.redlining.dto.UserResponse;
import com.redlining.entity.Plan;
import com.redlining.entity.User;
import com.redlining.repository.PlanRepository;
import com.redlining.repository.UserRepository;
import com.redlining.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final int AVATAR_PRESIGNED_EXPIRY_SECONDS = 7 * 24 * 3600; // 7 days

    private static final Pattern HAS_LETTER = Pattern.compile("[a-zA-Z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CaptchaServiceInterface captchaService;
    private final MinioService minioService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository, PlanRepository planRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, CaptchaServiceInterface captchaService, MinioService minioService,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.captchaService = captchaService;
        this.minioService = minioService;
        this.appProperties = appProperties;
    }

    private static boolean passwordValid(String password) {
        return password != null
                && password.length() >= 6
                && HAS_LETTER.matcher(password).find()
                && HAS_DIGIT.matcher(password).find();
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (!captchaService.validate(req.getCaptchaId(), req.getCaptchaCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired captcha");
        }
        if (!passwordValid(req.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 6 characters and contain both letters and numbers");
        }
        String username = req.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
        }
        String email = req.getEmail() == null || req.getEmail().isBlank()
                ? null
                : req.getEmail().trim().toLowerCase();
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setHashedPassword(passwordEncoder.encode(req.getPassword()));
        user.setEnabled(true);
        var defaultPlanOpt = planRepository.findByDefaultPlanTrue();
        if (defaultPlanOpt.isPresent()) {
            var plan = defaultPlanOpt.get();
            user.setPlanId(plan.getId());
            user.setQuotaRemaining(plan.getQuota());
            if ("subscription".equals(plan.getType()) && plan.getPeriod() != null) {
                user.setPeriodEndsAt(java.time.Instant.now().plus(
                    "year".equals(plan.getPeriod()) ? 365 : 30, java.time.temporal.ChronoUnit.DAYS));
            } else {
                user.setPeriodEndsAt(null);
            }
        }
        user = userRepository.save(user);
        String token = jwtUtil.createToken(user.getId());
        return new LoginResponse(token, new LoginResponse.UserInfo(user.getId(), user.getUsername(), user.getEmail()));
    }

    public LoginResponse login(String username, String password, String captchaId, String captchaCode) {
        if (!captchaService.validate(captchaId, captchaCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired captcha");
        }
        User user = userRepository.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        }
        if (!passwordEncoder.matches(password, user.getHashedPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        String token = jwtUtil.createToken(user.getId());
        return new LoginResponse(token, new LoginResponse.UserInfo(user.getId(), user.getUsername(), user.getEmail()));
    }

    public UserResponse getProfileResponse(User user) {
        if (user == null) return null;
        String avatarUrl = null;
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            avatarUrl = minioService.getPresignedUrl(user.getAvatarUrl(), AVATAR_PRESIGNED_EXPIRY_SECONDS);
        }
        UserResponse res = new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                avatarUrl, user.getRealName(), user.getOccupation());
        if (user.getPlanId() != null) {
            planRepository.findById(user.getPlanId()).ifPresent(plan -> {
                res.setPlanCode(plan.getCode());
                res.setPlanName(plan.getName());
                res.setPlanType(plan.getType());
                res.setQuotaRemaining(user.getQuotaRemaining());
                res.setQuotaTotal(plan.getQuota() <= 0 ? null : plan.getQuota());
                res.setPeriodEndsAt(user.getPeriodEndsAt() == null ? null : user.getPeriodEndsAt().toString());
            });
        }
        return res;
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (req.getRealName() != null) {
            user.setRealName(req.getRealName().trim().isEmpty() ? null : req.getRealName().trim());
        }
        if (req.getOccupation() != null) {
            user.setOccupation(req.getOccupation().trim().isEmpty() ? null : req.getOccupation().trim());
        }
        if (req.getEmail() != null) {
            String newEmail = req.getEmail().trim().isEmpty() ? null : req.getEmail().trim().toLowerCase();
            if (newEmail != null && !newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmailIgnoreCaseAndIdNot(newEmail, userId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该邮箱已被使用");
                }
                user.setEmail(newEmail);
            } else if (newEmail == null) {
                user.setEmail(null);
            }
        }
        user = userRepository.save(user);
        return getProfileResponse(user);
    }

    @Transactional
    public UserResponse updateAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }
        String contentType = file.getContentType();
        String[] allowed = appProperties.getMinio().getAvatar().getAllowedContentTypesArray();
        if (contentType == null || Arrays.stream(allowed).noneMatch(contentType::equalsIgnoreCase)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar must be image/jpeg or image/png");
        }
        long maxSize = appProperties.getMinio().getAvatar().getMaxSize();
        if (file.getSize() > maxSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar size must not exceed 5MB");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String ext = contentType.contains("png") ? ".png" : ".jpg";
        String objectKey = "avatars/" + userId + "_" + UUID.randomUUID() + ext;
        try {
            minioService.putObject(objectKey, file.getInputStream(), file.getSize(), contentType);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload avatar");
        }
        user.setAvatarUrl(objectKey);
        user = userRepository.save(user);
        return getProfileResponse(user);
    }
}
