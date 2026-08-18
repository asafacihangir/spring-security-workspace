package org.phoenix.springsecurity.repository;

import java.util.List;

import org.phoenix.springsecurity.domain.WorkLog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkLogRepository extends JpaRepository<WorkLog, Integer> {

	List<WorkLog> findByCreatedBy(int createdBy);

}
