package com.redlining.service;

import com.redlining.entity.OperationLog;
import com.redlining.entity.User;
import com.redlining.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    public void log(User user, String module, String action, HttpServletRequest request) {
        log(user, module, action, request, null);
    }

    public void log(User user, String module, String action, HttpServletRequest request, String requestBody) {
        OperationLog log = new OperationLog();
        log.setUserId(user != null ? user.getId() : null);
        log.setUsername(user != null ? (user.getUsername() != null ? user.getUsername() : user.getEmail()) : null);
        log.setModule(module);
        log.setAction(action);
        if (request != null) {
            log.setMethod(request.getMethod());
            log.setPath(request.getRequestURI());
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            log.setIp(ip);
        }
        if (requestBody != null && requestBody.length() > 2000) {
            requestBody = requestBody.substring(0, 2000) + "...";
        }
        log.setRequestBody(requestBody);
        operationLogRepository.save(log);
    }

    public Page<OperationLog> find(Long userId, String module, Instant start, Instant end, Pageable pageable) {
        if (userId != null) {
            return operationLogRepository.findByUserId(userId, pageable);
        }
        if (module != null && !module.isBlank()) {
            return operationLogRepository.findByModule(module, pageable);
        }
        if (start != null && end != null) {
            return operationLogRepository.findByCreatedAtBetween(start, end, pageable);
        }
        return operationLogRepository.findAll(pageable);
    }
}
