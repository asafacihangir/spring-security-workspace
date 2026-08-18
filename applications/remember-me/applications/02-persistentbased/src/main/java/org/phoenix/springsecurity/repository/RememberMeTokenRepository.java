package org.phoenix.springsecurity.repository;

import java.util.Date;
import java.util.List;

import org.phoenix.springsecurity.domain.PersistentLogin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface RememberMeTokenRepository extends JpaRepository<PersistentLogin, String> {

	PersistentLogin findBySeries(String series);

	List<PersistentLogin> findByUsername(String username);

	@Transactional
	int deleteByLastUsedBefore(Date expiration);

}
