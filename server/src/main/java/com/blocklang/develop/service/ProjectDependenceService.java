package com.blocklang.develop.service;

import java.util.List;
import java.util.Optional;

import com.blocklang.develop.constant.AppType;
import com.blocklang.develop.data.ProjectDependenceData;
import com.blocklang.develop.designer.data.WidgetRepo;
import com.blocklang.develop.model.ProjectDependence;
import com.blocklang.marketplace.model.ComponentRepo;

public interface ProjectDependenceService {

	/**
	 *  dev 仓库不存在 profile 一说
	 *  
	 * @param projectId
	 * @param componentRepoId
	 * @return
	 */
	Boolean devDependenceExists(Integer projectId, Integer componentRepoId);
	
	Boolean buildDependenceExists(Integer projectId, Integer componentRepoId, AppType appType, String profileName);

	/**
	 * 本方法会将依赖添加到默认的 Profile 下
	 * 
	 * @param projectId
	 * @param componentRepo
	 * @param user
	 * @return 如果保存失败则返回 <code>null</code>；否则返回保存后的项目依赖信息
	 */
	ProjectDependence save(Integer projectId, ComponentRepo componentRepo, Integer createUserId);

	List<ProjectDependenceData> findProjectDependences(Integer projectId);

	void delete(Integer dependenceId);

	ProjectDependence update(ProjectDependence dependence);

	Optional<ProjectDependence> findById(Integer dependenceId);

	List<WidgetRepo> findAllWidgets(Integer projectId);

}
