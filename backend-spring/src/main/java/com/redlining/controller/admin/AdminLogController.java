package com.redlining.controller.admin;

import com.redlining.entity.OperationLog;
import com.redlining.repository.OperationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final OperationLogRepository operationLogRepository;

    public AdminLogController(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @GetMapping
    public Page<OperationLog> list(@RequestParam(required = false) Long userId,
                                   @RequestParam(required = false) String module,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
                                   @PageableDefault(size = 50) Pageable pageable) {
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
