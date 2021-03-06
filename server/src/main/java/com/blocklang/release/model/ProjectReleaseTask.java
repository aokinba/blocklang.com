package com.blocklang.release.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.blocklang.core.model.PartialOperateFields;
import com.blocklang.release.constant.ReleaseResult;
import com.blocklang.release.constant.converter.ReleaseResultConverter;

@Entity
@Table(name = "project_release_task", uniqueConstraints = @UniqueConstraint(columnNames = { "project_id", "version" }))
public class ProjectReleaseTask extends PartialOperateFields{

	private static final long serialVersionUID = -2695309681346878878L;

	@Column(name = "project_id", nullable = false)
	private Integer projectId;
	
	@Column(name = "version", nullable = false, length = 32)
	private String version;
	
	@Column(nullable = false, length = 64)
	private String title;
	
	private String description;
	
	@Column(name = "jdk_release_id", nullable = false)
	private Integer jdkReleaseId;
	
	@Column(name = "start_time", nullable = false)
	private LocalDateTime startTime;
	
	@Column(name = "end_time")
	private LocalDateTime endTime;
	
	@Convert(converter = ReleaseResultConverter.class)
	@Column(name = "release_result", length = 2, nullable = false)	
	private ReleaseResult releaseResult;
	
	@Column(name = "log_file_name", length = 255)
	private String logFileName;
	
	@Transient
	private String jdkName;
	@Transient
	private String jdkVersion;
	@Transient
	private String createUserName;
	@Transient
	private String createUserAvatarUrl;

	public Integer getProjectId() {
		return projectId;
	}

	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getJdkReleaseId() {
		return jdkReleaseId;
	}

	public void setJdkReleaseId(Integer jdkReleaseId) {
		this.jdkReleaseId = jdkReleaseId;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public ReleaseResult getReleaseResult() {
		return releaseResult;
	}

	public void setReleaseResult(ReleaseResult releaseResult) {
		this.releaseResult = releaseResult;
	}

	public String getJdkName() {
		return jdkName;
	}

	public void setJdkName(String jdkName) {
		this.jdkName = jdkName;
	}

	public String getJdkVersion() {
		return jdkVersion;
	}

	public void setJdkVersion(String jdkVersion) {
		this.jdkVersion = jdkVersion;
	}

	public String getCreateUserName() {
		return createUserName;
	}

	public void setCreateUserName(String createUserName) {
		this.createUserName = createUserName;
	}

	public String getCreateUserAvatarUrl() {
		return createUserAvatarUrl;
	}

	public void setCreateUserAvatarUrl(String createUserAvatarUrl) {
		this.createUserAvatarUrl = createUserAvatarUrl;
	}

	public String getLogFileName() {
		return logFileName;
	}

	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}
	
}
