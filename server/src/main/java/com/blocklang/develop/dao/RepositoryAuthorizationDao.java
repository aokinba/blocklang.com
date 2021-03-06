package com.blocklang.develop.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blocklang.develop.model.RepositoryAuthorization;

public interface RepositoryAuthorizationDao extends JpaRepository<RepositoryAuthorization, Integer> {

	List<RepositoryAuthorization> findAllByUserId(Integer userId);

	List<RepositoryAuthorization> findAllByUserIdAndRepositoryId(Integer userId, Integer repositoryId);

}
