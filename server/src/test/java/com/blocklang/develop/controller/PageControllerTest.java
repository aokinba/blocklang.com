package com.blocklang.develop.controller;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;

import com.blocklang.core.constant.Constant;
import com.blocklang.core.model.UserInfo;
import com.blocklang.core.test.AbstractControllerTest;
import com.blocklang.develop.constant.AccessLevel;
import com.blocklang.develop.constant.AppType;
import com.blocklang.develop.constant.ProjectResourceType;
import com.blocklang.develop.data.CheckPageKeyParam;
import com.blocklang.develop.data.CheckPageNameParam;
import com.blocklang.develop.data.NewPageParam;
import com.blocklang.develop.designer.data.Page;
import com.blocklang.develop.model.Project;
import com.blocklang.develop.model.ProjectAuthorization;
import com.blocklang.develop.model.ProjectResource;
import com.blocklang.develop.service.ProjectAuthorizationService;
import com.blocklang.develop.service.ProjectResourceService;
import com.blocklang.develop.service.ProjectService;

import io.restassured.http.ContentType;

@WebMvcTest(PageController.class)
public class PageControllerTest extends AbstractControllerTest{
	
	@MockBean
	private ProjectService projectService;
	@MockBean
	private ProjectAuthorizationService projectAuthorizationService;
	@MockBean
	private ProjectResourceService projectResourceService;

	
	@Test
	public void check_key_user_not_login() {
		CheckPageKeyParam param = new CheckPageKeyParam();
		param.setKey("key");
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-key", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN)
			.body(equalTo(""));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void check_key_user_login_but_project_not_exist() {
		CheckPageKeyParam param = new CheckPageKeyParam();
		param.setKey("key");
		
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.empty());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-key", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_NOT_FOUND)
			.body(equalTo(""));
	}
	
	// 用户无权访问的公开项目
	// 没有为用户配置该项目的任何权限
	@WithMockUser(username = "jack")
	@Test
	public void check_key_user_can_not_read_public_project() {
		CheckPageKeyParam param = new CheckPageKeyParam();
		param.setKey("key");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.emptyList());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-key", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN)
			.body(equalTo(""));
	}
	
	// 用户对公开项目只有 read 权限
	@WithMockUser(username = "jack")
	@Test
	public void check_key_login_user_can_not_write_public_project() {
		CheckPageKeyParam param = new CheckPageKeyParam();
		param.setKey("key");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.READ);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-key", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN)
			.body(equalTo(""));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void check_key_is_blank() {
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		CheckPageKeyParam param = new CheckPageKeyParam();
		param.setKey(" ");
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-key", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("名称不能为空"),
					"errors.key.size()", is(1));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void check_key_is_invalid() {
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		CheckPageKeyParam param = new CheckPageKeyParam();
		param.setKey("中文");
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-key", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("只允许字母、数字、中划线(-)、下划线(_)"),
					"errors.key.size()", is(1));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void check_key_is_used_at_root() {
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		ProjectResource resource = new ProjectResource();
		when(projectResourceService.findByKey(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource));
		
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(new ProjectResource()));
		
		CheckPageKeyParam param = new CheckPageKeyParam();
		param.setKey("a-used-key");
		param.setParentId(Constant.TREE_ROOT_ID);
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-key", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("根目录下已存在名称<strong>a-used-key</strong>"),
					"errors.key.size()", is(1));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void check_key_is_used_at_sub() {
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		Integer parentId = 1;
		ProjectResource resource = new ProjectResource();
		resource.setParentId(parentId);
		when(projectResourceService.findByKey(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource));
		
		ProjectResource parentResource = new ProjectResource();
		parentResource.setId(parentId);
		parentResource.setName("二级目录");
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(parentResource));
		
		CheckPageKeyParam param = new CheckPageKeyParam();
		param.setKey("a-used-key");
		param.setParentId(parentId);
		param.setAppType(AppType.WEB.getKey());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-key", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("二级目录下已存在名称<strong>a-used-key</strong>"),
					"errors.key.size()", is(1));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void check_key_is_passed() {
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		when(projectResourceService.findByKey(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.empty());
		
		CheckPageKeyParam param = new CheckPageKeyParam();
		param.setKey("key");
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-key", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_OK)
			.body(equalTo("{}"));
	}
	
	@Test
	public void check_name_user_not_login() {
		CheckPageNameParam param = new CheckPageNameParam();
		param.setName("name");
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-name", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN)
			.body(equalTo(""));
	}

	@WithMockUser(username = "jack")
	@Test
	public void check_name_user_login_but_project_not_exist() {
		CheckPageNameParam param = new CheckPageNameParam();
		param.setName("name");
		
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.empty());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-name", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_NOT_FOUND)
			.body(equalTo(""));
	}

	@WithMockUser(username = "jack")
	@Test
	public void check_name_login_user_can_not_write_public_project() {
		CheckPageNameParam param = new CheckPageNameParam();
		param.setName("name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.READ);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-name", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN)
			.body(equalTo(""));
	}

	@WithMockUser(username = "jack")
	@Test
	public void check_name_can_be_null() {
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		ProjectResource resource = new ProjectResource();
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource));
		
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(new ProjectResource()));
		
		CheckPageNameParam param = new CheckPageNameParam();
		param.setName(null);
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-name", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_OK);
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void check_name_is_used_at_root() {
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		ProjectResource resource = new ProjectResource();
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource));
		
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(new ProjectResource()));
		
		CheckPageNameParam param = new CheckPageNameParam();
		param.setName("a-used-name");
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-name", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.name", hasItem("根目录下已存在备注<strong>a-used-name</strong>"),
					"errors.name.size()", is(1));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void check_name_is_used_at_sub() {
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		Integer groupId = 1;
		ProjectResource resource = new ProjectResource();
		resource.setParentId(groupId);
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource));
		
		ProjectResource parentResource = new ProjectResource();
		parentResource.setId(groupId);
		parentResource.setName("二级目录");
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(parentResource));
		
		CheckPageNameParam param = new CheckPageNameParam();
		param.setName("a-used-name");
		param.setParentId(groupId);
		param.setAppType(AppType.WEB.getKey());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-name", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.name", hasItem("二级目录下已存在备注<strong>a-used-name</strong>"),
					"errors.name.size()", is(1));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void check_name_is_passed() {
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		project.setCreateUserId(1);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));

		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.empty());
		
		CheckPageNameParam param = new CheckPageNameParam();
		param.setName("name");
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages/check-name", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_OK)
			.body(equalTo("{}"));
	}
	
	@Test
	public void new_page_user_not_login() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey("key");
		param.setName("name");
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN)
			.body(equalTo(""));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void new_page_user_login_but_project_not_exist() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey("key");
		param.setName("name");
		
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.empty());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_NOT_FOUND)
			.body(equalTo(""));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void new_page_user_can_not_read_public_project() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey("key");
		param.setName("name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.emptyList());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN)
			.body(equalTo(""));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void new_page_login_user_can_not_write_public_project() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey("key");
		param.setName("name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.READ);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN)
			.body(equalTo(""));
	}

	@WithMockUser(username = "jack")
	@Test
	public void new_page_check_key_is_null_and_name_is_null() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey(null);
		param.setName(null);
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.empty());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("名称不能为空"),
					"errors.key.size()", is(1),
					"errors.name", is(nullValue()));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void new_page_check_key_is_blank_and_name_passed() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey(" ");
		param.setName("name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.empty());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("名称不能为空"),
					"errors.key.size()", is(1),
					"errors.name", is(nullValue()));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void new_page_check_key_is_blank_and_name_is_used() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey(" ");
		param.setName("a-used-name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		ProjectResource resource = new ProjectResource();
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource));
		
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(new ProjectResource()));
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("名称不能为空"),
					"errors.key.size()", is(1),
					"errors.name", hasItem("根目录下已存在备注<strong>a-used-name</strong>"),
					"errors.name.size()", is(1));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void new_page_check_key_is_invalid_and_name_is_passed() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey("中文");
		param.setName("name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.empty());
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("只允许字母、数字、中划线(-)、下划线(_)"),
					"errors.key.size()", is(1),
					"errors.name", is(nullValue()));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void new_page_check_key_is_invalid_and_name_is_used() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey("中文");
		param.setName("a-used-name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		ProjectResource resource = new ProjectResource();
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource));
		
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(new ProjectResource()));
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("只允许字母、数字、中划线(-)、下划线(_)"),
					"errors.key.size()", is(1),
					"errors.name", hasItem("根目录下已存在备注<strong>a-used-name</strong>"),
					"errors.name.size()", is(1));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void new_page_check_key_is_used_and_name_is_used() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey("a-used-key");
		param.setName("a-used-name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		ProjectResource resource1 = new ProjectResource();
		when(projectResourceService.findByKey(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource1));
		
		ProjectResource resource2 = new ProjectResource();
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource2));
		
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(new ProjectResource()));
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("根目录下已存在名称<strong>a-used-key</strong>"),
					"errors.key.size()", is(1),
					"errors.name", hasItem("根目录下已存在备注<strong>a-used-name</strong>"),
					"errors.name.size()", is(1));
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void new_page_check_key_is_used_and_name_is_pass() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey("a-used-key");
		param.setName("name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		ProjectResource resource1 = new ProjectResource();
		when(projectResourceService.findByKey(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.of(resource1));
		
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.empty());
		
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(new ProjectResource()));
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
			.body("errors.key", hasItem("根目录下已存在名称<strong>a-used-key</strong>"),
					"errors.key.size()", is(1),
					"errors.name", is(nullValue()));
	}
	
	// 校验都通过后，才保存。
	@WithMockUser(username = "jack")
	@Test
	public void new_page_success() {
		NewPageParam param = new NewPageParam();
		param.setParentId(Constant.TREE_ROOT_ID);
		param.setAppType(AppType.WEB.getKey());
		param.setKey("key");
		param.setName("name");
		
		Project project = new Project();
		project.setId(1);
		project.setCreateUserId(1);
		project.setCreateUserName("jack");
		project.setName("project");
		project.setIsPublic(true); // 公开项目
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		when(projectResourceService.findByKey(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.empty());
		when(projectResourceService.findByName(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(Optional.empty());
		
		UserInfo currentUser = new UserInfo();
		currentUser.setLoginName("jack");
		currentUser.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(currentUser));
		
		ProjectResource savedResource = new ProjectResource();
		savedResource.setProjectId(project.getId());
		savedResource.setParentId(param.getParentId());
		savedResource.setKey("key");
		savedResource.setName("name");
		savedResource.setId(1);
		savedResource.setSeq(1);
		savedResource.setAppType(AppType.WEB);
		savedResource.setResourceType(ProjectResourceType.PAGE);
		when(projectResourceService.insert(any(), any())).thenReturn(savedResource);
		
		given()
			.contentType(ContentType.JSON)
			.body(param)
		.when()
			.post("/projects/{owner}/{projectName}/pages", "jack", "project")
		.then()
			.statusCode(HttpStatus.SC_CREATED)
			.body("key", equalTo("key"),
				  "name", equalTo("name"),
				  "id", is(notNullValue()));
	}

	@Test
	public void get_page_project_not_found() {
		String owner = "owner";
		String projectName = "not-exist-project";
		
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.empty());

		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/projects/{owner}/{projectName}/pages/{pagePath}", owner, projectName, "a")
		.then()
			.statusCode(HttpStatus.SC_NOT_FOUND);
	}
	
	@Test
	public void get_page_anonymous_user_can_not_access_private_project() {
		String owner = "owner";
		String projectName = "private-project";
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(false);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));

		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/projects/{owner}/{projectName}/pages/{pagePath}", owner, projectName, "a")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN);
	}
	
	@Test
	public void get_page_page_not_exist() {
		String owner = "owner";
		String projectName = "project";
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(true);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		when(projectResourceService.findParentGroupsByParentPath(anyInt(), anyString())).thenReturn(Collections.emptyList());

		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/projects/{owner}/{projectName}/pages/{pagePath}", owner, projectName, "a")
		.then()
			.statusCode(HttpStatus.SC_NOT_FOUND);
	}
	
	@Test
	public void get_page_success() {
		String owner = "owner";
		String projectName = "project";
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(true);
		when(projectService.find(anyString(), anyString())).thenReturn(Optional.of(project));
		
		ProjectResource resource = new ProjectResource();
		resource.setId(1);
		resource.setKey("a");
		resource.setName("A");
		when(projectResourceService.findParentGroupsByParentPath(anyInt(), anyString())).thenReturn(Collections.singletonList(resource));

		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/projects/{owner}/{projectName}/pages/{pagePath}", owner, projectName, "a")
		.then()
			.statusCode(HttpStatus.SC_OK)
			.body("projectResource.id", equalTo(1),
					  "parentGroups.size", equalTo(1),
					  "parentGroups[0].name", equalTo("A"),
					  "parentGroups[0].path", equalTo("/a"));
	}
	
	@Test
	public void update_page_model_forbidden_anonymous_user() {
		Map<String, Object> model = new HashMap<String, Object>();
		
		given()
			.contentType(ContentType.JSON)
			.body(model)
		.when()
			.put("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN);
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void update_page_model_page_not_found() {
		Map<String, Object> model = new HashMap<String, Object>();
		
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.empty());
		
		given()
			.contentType(ContentType.JSON)
			.body(model)
		.when()
			.put("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_NOT_FOUND);
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void update_page_model_login_user_has_no_permission() {
		Map<String, Object> model = new HashMap<String, Object>();
		
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		given()
			.contentType(ContentType.JSON)
			.body(model)
		.when()
			.put("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN);
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void update_page_model_login_user_has_read_permission() {
		Map<String, Object> model = new HashMap<String, Object>();
		
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.READ);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		given()
			.contentType(ContentType.JSON)
			.body(model)
		.when()
			.put("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN);
	}
	
	@WithMockUser(username = "jack")
	@Test
	public void update_page_model_success() {
		Map<String, Object> model = new HashMap<String, Object>();
		
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		given()
			.contentType(ContentType.JSON)
			.body(model)
		.when()
			.put("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_CREATED);
		
		verify(projectResourceService).updatePageModel(any());
	}

	@Test
	public void get_page_model_page_not_found() {
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.empty());
		
		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_NOT_FOUND);
	}
	
	// 匿名用户能访问公开项目
	@Test
	public void get_page_model_anonymous_user_can_access_public_project() {
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(true);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		Page pageModel = new Page();
		when(projectResourceService.getPageModel(anyInt())).thenReturn(Optional.of(pageModel));
		
		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_OK)
			.body(equalTo("{}"));
	}
	
	// 匿名用户不能访问私有项目
	@Test
	public void get_page_model_anonymous_user_can_access_private_project() {
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(false);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN);
	}
	
	// 登录用户能访问公开项目
	@Test
	public void get_page_model_login_user_can_access_public_project() {
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(true);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		Page pageModel = new Page();
		when(projectResourceService.getPageModel(anyInt())).thenReturn(Optional.of(pageModel));
		
		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_OK)
			.body(equalTo("{}"));
	}
	
	// 登录用户不能访问没有权限的私有项目
	@WithMockUser("jack")
	@Test
	public void get_page_model_login_user_forbidden_no_permission_private_project() {
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(false);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN);
	}
	
	// 登录用户能访问有读权限的私有项目
	@WithMockUser("jack")
	@Test
	public void get_page_model_login_user_can_access_has_read_permission_private_project() {
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(false);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.READ);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		Page pageModel = new Page();
		when(projectResourceService.getPageModel(anyInt())).thenReturn(Optional.of(pageModel));
		
		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_OK)
			.body(equalTo("{}"));
	}
	
	// 登录用户能访问有写权限的私有项目
	@WithMockUser("jack")
	@Test
	public void get_page_model_login_user_can_access_has_write_permission_private_project() {
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(false);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.WRITE);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		Page pageModel = new Page();
		when(projectResourceService.getPageModel(anyInt())).thenReturn(Optional.of(pageModel));
		
		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_OK)
			.body(equalTo("{}"));
	}
	
	// 登录用户能访问有管理权限的私有项目
	@WithMockUser("jack")
	@Test
	public void get_page_model_login_user_can_access_has_admin_permission_private_project() {
		UserInfo user = new UserInfo();
		user.setId(1);
		when(userService.findByLoginName(anyString())).thenReturn(Optional.of(user));
		
		ProjectResource page = new ProjectResource();
		page.setProjectId(1);
		when(projectResourceService.findById(anyInt())).thenReturn(Optional.of(page));
		
		Project project = new Project();
		project.setId(1);
		project.setIsPublic(false);
		when(projectService.findById(anyInt())).thenReturn(Optional.of(project));
		
		ProjectAuthorization auth = new ProjectAuthorization();
		auth.setUserId(1);
		auth.setProjectId(1);
		auth.setAccessLevel(AccessLevel.ADMIN);
		when(projectAuthorizationService.findAllByUserIdAndProjectId(anyInt(), anyInt())).thenReturn(Collections.singletonList(auth));
		
		Page pageModel = new Page();
		when(projectResourceService.getPageModel(anyInt())).thenReturn(Optional.of(pageModel));
		
		given()
			.contentType(ContentType.JSON)
		.when()
			.get("/pages/{pageId}/model", "1")
		.then()
			.statusCode(HttpStatus.SC_OK)
			.body(equalTo("{}"));
	}
}
