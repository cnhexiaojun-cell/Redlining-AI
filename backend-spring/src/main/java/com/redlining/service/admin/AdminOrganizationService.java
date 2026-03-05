package com.redlining.service.admin;

import com.redlining.dto.admin.OrganizationCreateRequest;
import com.redlining.dto.admin.OrganizationDto;
import com.redlining.entity.Organization;
import com.redlining.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminOrganizationService {

    private final OrganizationRepository organizationRepository;

    public AdminOrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public List<OrganizationDto> getTree(Long parentId) {
        List<Organization> list = parentId == null
                ? organizationRepository.findByParentIdIsNullOrderBySortOrderAsc()
                : organizationRepository.findByParentIdOrderBySortOrderAsc(parentId);
        List<OrganizationDto> result = new ArrayList<>();
        for (Organization o : list) {
            OrganizationDto dto = toDto(o);
            dto.setChildren(getTree(o.getId()));
            result.add(dto);
        }
        return result;
    }

    public List<OrganizationDto> getFlatList() {
        return organizationRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public OrganizationDto create(OrganizationCreateRequest req) {
        if (organizationRepository.existsByCode(req.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "机构编码已存在");
        }
        Organization o = new Organization();
        o.setName(req.getName());
        o.setCode(req.getCode());
        o.setParentId(req.getParentId());
        o.setSortOrder(req.getSortOrder());
        o = organizationRepository.save(o);
        return toDto(o);
    }

    @Transactional
    public OrganizationDto update(Long id, OrganizationCreateRequest req) {
        Organization o = organizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "机构不存在"));
        if (organizationRepository.existsByCodeAndIdNot(req.getCode(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "机构编码已存在");
        }
        o.setName(req.getName());
        o.setCode(req.getCode());
        o.setParentId(req.getParentId());
        o.setSortOrder(req.getSortOrder());
        o = organizationRepository.save(o);
        return toDto(o);
    }

    @Transactional
    public void delete(Long id) {
        if (!organizationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "机构不存在");
        }
        organizationRepository.deleteById(id);
    }

    private OrganizationDto toDto(Organization o) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(o.getId());
        dto.setName(o.getName());
        dto.setCode(o.getCode());
        dto.setParentId(o.getParentId());
        dto.setSortOrder(o.getSortOrder());
        dto.setCreatedAt(o.getCreatedAt());
        return dto;
    }
}
