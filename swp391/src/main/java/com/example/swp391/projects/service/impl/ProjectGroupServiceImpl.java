package com.example.swp391.projects.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.exception.BadRequestException;
import com.example.swp391.exception.NotFoundException;
import com.example.swp391.projects.dto.request.CreateProjectGroupRequest;
import com.example.swp391.projects.dto.response.ProjectGroupResponse;
import com.example.swp391.projects.entity.ProjectGroup;
import com.example.swp391.projects.enums.GroupStatus;
import com.example.swp391.projects.repository.ProjectGroupRepository;
import com.example.swp391.projects.service.IProjectGroupService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectGroupServiceImpl implements IProjectGroupService {

    private final ProjectGroupRepository projectGroupRepository;
    private final AccountRepository accountRepository;

    @Override
    public ProjectGroupResponse createProjectGroup(CreateProjectGroupRequest request) {

        Account lecturer = accountRepository.findById(request.getLecturerId())
                .orElseThrow(() -> new NotFoundException("Lecturer not found"));

        if (!"LECTURER".equals(lecturer.getRole().getName())) {
            throw new BadRequestException("Assigned account is not a lecturer");
        }

        boolean exists = projectGroupRepository
                .existsByGroupNameAndSemester(
                        request.getGroupName(),
                        request.getSemester()
                );

        if (exists) {
            throw new BadRequestException("Group name already exists in this semester");
        }

        Account admin = new SecurityUtil(accountRepository).getCurrentAccount();

        ProjectGroup group = ProjectGroup.builder()
                .groupName(request.getGroupName())
                .semester(request.getSemester())
                .lecturer(lecturer)
                .createdBy(admin)
                .status(GroupStatus.OPEN)
                .build();

        ProjectGroup savedGroup = projectGroupRepository.save(group);

        return mapToResponse(savedGroup);
    }

    private ProjectGroupResponse mapToResponse(ProjectGroup group) {
        ProjectGroupResponse res = new ProjectGroupResponse();
        res.setId(group.getId());
        res.setGroupName(group.getGroupName());
        res.setSemester(group.getSemester());

        res.setLecturerId(group.getLecturer().getId());
        res.setLecturerUsername(group.getLecturer().getUsername());
        res.setLecturerEmail(group.getLecturer().getEmail());

        res.setStatus(group.getStatus());
        return res;
    }

}
