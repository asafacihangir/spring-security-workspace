package org.phoenix.springsecurity.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.phoenix.springsecurity.domain.Role;
import org.phoenix.springsecurity.domain.User;
import org.phoenix.springsecurity.domain.WorkLog;
import org.phoenix.springsecurity.repository.WorkLogRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class WorkLogService {

    private final WorkLogRepository workLogRepository;
    private final UserContext userContext;

    public WorkLogService(WorkLogRepository workLogRepository, UserContext userContext) {
        this.workLogRepository = workLogRepository;
        this.userContext = userContext;
    }

    @Transactional(readOnly = true)
    public List<WorkLog> getAll() {
        return workLogRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<WorkLog> findMine() {
        return workLogRepository.findByCreatedBy(userContext.getCurrentUser().getId());
    }

    @Transactional
    public int create(String explanation) {
        WorkLog workLog = new WorkLog();
        workLog.setExplanation(explanation);
        workLog.setCreatedBy(userContext.getCurrentUser().getId());
        return workLogRepository.save(workLog).getId();
    }

    @Transactional(readOnly = true)
    public WorkLog get(int id) {
        WorkLog workLog = workLogRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No work log with id " + id));
        User currentUser = userContext.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isAdmin && !Objects.equals(workLog.getCreatedBy(), currentUser.getId())) {
            throw new AccessDeniedException("Not allowed to view work log " + id);
        }
        return workLog;
    }

}
