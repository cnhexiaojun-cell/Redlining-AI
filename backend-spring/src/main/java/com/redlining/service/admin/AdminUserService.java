package com.redlining.service.admin;

import com.redlining.dto.admin.UserAdminDto;
import com.redlining.entity.User;
import com.redlining.entity.UserRole;
import com.redlining.repository.OrganizationRepository;
import com.redlining.repository.UserRepository;
import com.redlining.repository.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, UserRoleRepository userRoleRepository,
                            OrganizationRepository organizationRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<UserAdminDto> list(Long organizationId, Boolean enabled, Pageable pageable) {
        Page<User> page;
        if (organizationId != null) {
            page = userRepository.findByOrganizationId(organizationId, pageable);
        } else if (enabled != null) {
            page = userRepository.findByEnabled(enabled, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return page.map(this::toDto);
    }

    public UserAdminDto get(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        UserAdminDto dto = toDto(user);
        dto.setRoleIds(userRoleRepository.findByUserId(id).stream().map(UserRole::getRoleId).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public UserAdminDto create(String username, String email, String password, String realName, Long organizationId, List<Long> roleIds) {
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
        }
        if (email != null && !email.isBlank() && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email != null && !email.isBlank() ? email : null);
        user.setHashedPassword(passwordEncoder.encode(password != null ? password : "changeme"));
        user.setRealName(realName);
        user.setEnabled(true);
        user.setSuperAdmin(false);
        user.setOrganizationId(organizationId);
        user = userRepository.save(user);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                userRoleRepository.save(ur);
            }
        }
        return get(user.getId());
    }

    @Transactional
    public UserAdminDto update(Long id, String email, String realName, Boolean enabled, Long organizationId, List<Long> roleIds) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能修改超级管理员");
        }
        if (email != null && userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱已存在");
        }
        if (email != null) user.setEmail(email.isBlank() ? null : email);
        if (realName != null) user.setRealName(realName);
        if (enabled != null) user.setEnabled(enabled);
        if (organizationId != null) user.setOrganizationId(organizationId);
        userRepository.save(user);
        if (roleIds != null) {
            userRoleRepository.deleteByUserId(id);
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                userRoleRepository.save(ur);
            }
        }
        return get(id);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能修改超级管理员密码");
        }
        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private UserAdminDto toDto(User u) {
        UserAdminDto dto = new UserAdminDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setRealName(u.getRealName());
        dto.setEnabled(u.isEnabled());
        dto.setSuperAdmin(u.isSuperAdmin());
        dto.setOrganizationId(u.getOrganizationId());
        if (u.getOrganizationId() != null) {
            organizationRepository.findById(u.getOrganizationId()).ifPresent(o -> dto.setOrganizationName(o.getName()));
        }
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }
}
