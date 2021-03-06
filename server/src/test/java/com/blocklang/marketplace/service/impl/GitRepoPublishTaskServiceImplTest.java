package com.blocklang.marketplace.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.blocklang.core.dao.UserDao;
import com.blocklang.core.model.UserInfo;
import com.blocklang.core.test.AbstractServiceTest;
import com.blocklang.marketplace.constant.PublishType;
import com.blocklang.marketplace.model.GitRepoPublishTask;
import com.blocklang.marketplace.service.GitRepoPublishTaskService;
import com.blocklang.release.constant.ReleaseResult;

public class GitRepoPublishTaskServiceImplTest extends AbstractServiceTest{

	@Autowired
	private GitRepoPublishTaskService gitRepoPublishTaskService;
	@Autowired
	private UserDao userDao;
	
	@Test
	public void save_success() {
		GitRepoPublishTask task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo");
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(1);
		
		GitRepoPublishTask savedTask = gitRepoPublishTaskService.save(task);
		assertThat(savedTask.getId()).isNotNull();
		assertThat(savedTask.getSeq()).isEqualTo(1);
	}
	
	@Test
	public void save_seq_increase() {
		GitRepoPublishTask task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo");
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(1);
		GitRepoPublishTask savedTask = gitRepoPublishTaskService.save(task);
		
		task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo");
		task.setStartTime(LocalDateTime.now());
		task.setPublishType(PublishType.UPGRADE);
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(1);
		
		savedTask = gitRepoPublishTaskService.save(task);
		assertThat(savedTask.getId()).isNotNull();
		assertThat(savedTask.getSeq()).isEqualTo(2);
	}
	
	@Test
	public void save_seq_start_from_1() {
		GitRepoPublishTask task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-A");
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(1);
		GitRepoPublishTask savedTask = gitRepoPublishTaskService.save(task);
		
		assertThat(savedTask.getSeq()).isEqualTo(1);
		
		task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-B");
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(1);
		
		savedTask = gitRepoPublishTaskService.save(task);
		assertThat(savedTask.getSeq()).isEqualTo(1);
	}
	
	@Test
	public void find_user_publishing_no_data() {
		List<GitRepoPublishTask> result = gitRepoPublishTaskService.findUserPublishingTasks(1);
		assertThat(result).isEmpty();;
	}
	
	@Test
	public void find_user_publishing_tasks_started() {
		Integer createUserId = 1;
		
		GitRepoPublishTask task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-1");
		task.setSeq(1);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(createUserId);
		gitRepoPublishTaskService.save(task);
		
		task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-2");
		task.setSeq(1);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(2);
		gitRepoPublishTaskService.save(task);
		
		List<GitRepoPublishTask> result = gitRepoPublishTaskService.findUserPublishingTasks(1);
		assertThat(result).hasSize(1);
	}
	
	@Test
	public void find_user_publishing_tasks_inited_or_failed_or_passed_or_canceled() {
		Integer createUserId = 1;
		
		GitRepoPublishTask task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-1");
		task.setSeq(1);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.INITED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(createUserId);
		gitRepoPublishTaskService.save(task);
		
		task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-2");
		task.setSeq(1);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.FAILED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(createUserId);
		gitRepoPublishTaskService.save(task);
		
		task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-3");
		task.setSeq(1);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.PASSED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(createUserId);
		gitRepoPublishTaskService.save(task);
		
		task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-4");
		task.setSeq(1);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.CANCELED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(createUserId);
		gitRepoPublishTaskService.save(task);
		
		List<GitRepoPublishTask> result = gitRepoPublishTaskService.findUserPublishingTasks(1);
		assertThat(result).isEmpty();
	}
	
	@Test
	public void find_user_publishing_tasks_order_by_create_time_desc() {
		Integer createUserId = 1;
		
		GitRepoPublishTask task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-1");
		task.setSeq(1);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now().minusSeconds(1));
		task.setCreateUserId(createUserId);
		gitRepoPublishTaskService.save(task);
		
		task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-2");
		task.setSeq(1);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now());
		task.setCreateUserId(createUserId);
		gitRepoPublishTaskService.save(task);
		
		List<GitRepoPublishTask> result = gitRepoPublishTaskService.findUserPublishingTasks(createUserId);
		assertThat(result).hasSize(2).isSortedAccordingTo(Comparator.comparing(GitRepoPublishTask::getCreateTime).reversed());
	}
	
	@Test
	public void find_by_id_get_create_user_name() {
		UserInfo user = new UserInfo();
		user.setLoginName("jack");
		user.setAvatarUrl("avatar_url");
		user.setCreateTime(LocalDateTime.now());
		UserInfo savedUser = userDao.save(user);
		
		GitRepoPublishTask task = new GitRepoPublishTask();
		task.setGitUrl("https://a.com/jack/repo-1");
		task.setSeq(1);
		task.setStartTime(LocalDateTime.now());
		task.setPublishResult(ReleaseResult.STARTED);
		task.setCreateTime(LocalDateTime.now().minusSeconds(1));
		task.setCreateUserId(savedUser.getId());
		assertThat(gitRepoPublishTaskService.findById(gitRepoPublishTaskService.save(task).getId()).get().getCreateUserName()).isEqualTo("jack");
	}
}
