package com.blocklang.develop.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blocklang.core.constant.CmPropKey;
import com.blocklang.core.exception.NoAuthorizationException;
import com.blocklang.core.exception.ResourceNotFoundException;
import com.blocklang.core.service.PropertyService;
import com.blocklang.develop.data.ProjectDependenceData;
import com.blocklang.develop.designer.data.Dependence;
import com.blocklang.develop.designer.data.PageModel;
import com.blocklang.develop.designer.data.RepoWidgetList;
import com.blocklang.develop.model.Repository;
import com.blocklang.develop.model.RepositoryResource;
import com.blocklang.develop.service.ProjectDependenceService;
import com.blocklang.develop.service.RepositoryResourceService;
import com.blocklang.marketplace.data.MarketplaceStore;
import com.blocklang.marketplace.model.ComponentRepo;

/**
 * 页面设计器专用的控制器放在此处集中维护。
 * 
 * @author jinzw
 *
 */
@RestController
public class PageDesignerController extends AbstractRepositoryController {
	
	@Autowired
	private ProjectDependenceService projectDependenceService;
	@Autowired
	private RepositoryResourceService repositoryResourceService;
	@Autowired
	private PropertyService propertyService;
	
	/**
	 * 与 {@link ProjectDependenceController#getDependence(Principal, String, String)}} 功能类似，
	 * 但一个是在项目依赖的维护页面中使用的，一个是在页面设计器中使用的。
	 * 
	 * @param principal
	 * @param projectId
	 * @param repo
	 * @return
	 */
	@GetMapping("/designer/projects/{projectId}/dependences")
	public ResponseEntity<List<Dependence>> listProjectDependences(
			Principal principal,
			@PathVariable Integer projectId, // 此处的 projectId 就是仓库中的分组 id
			@RequestParam String repo) {
		
		if(!repo.equalsIgnoreCase("ide")) {
			throw new UnsupportedOperationException("当前仅支持获取 ide 依赖。");
		}
		RepositoryResource project = repositoryResourceService.findById(projectId).orElseThrow(ResourceNotFoundException::new);
		Repository repository = repositoryService.findById(project.getRepositoryId()).orElseThrow(ResourceNotFoundException::new);
		repositoryPermissionService.canRead(principal, repository).orElseThrow(NoAuthorizationException::new);
		
		// 分开读取标准库和普通库
		List<ProjectDependenceData> dependences= projectDependenceService.findIdeDependences(project);
		dependences.addAll(projectDependenceService.findStdIdeDependences(project));
		List<Dependence> result = dependences.stream().map(item -> {
			Dependence dependence = new Dependence();

			ComponentRepo componentRepo = item.getComponentRepo();
			dependence.setId(componentRepo.getId());
			dependence.setGitRepoWebsite(componentRepo.getGitRepoWebsite());
			dependence.setGitRepoOwner(componentRepo.getGitRepoOwner());
			dependence.setGitRepoName(componentRepo.getGitRepoName());
			dependence.setCategory(componentRepo.getCategory().getValue());
			dependence.setStd(componentRepo.isStd());
			dependence.setVersion(item.getComponentRepoVersion().getVersion());
			dependence.setApiRepoId(item.getApiRepo().getId());
			
			return dependence;
		}).collect(Collectors.toList());
		return ResponseEntity.ok(result);
		
//		Repository project = repositoryService.findById(projectId).orElseThrow(ResourceNotFoundException::new);
//		repositoryPermissionService.canRead(principal, project).orElseThrow(NoAuthorizationException::new);
//
//		String stdIdeWidgetGitUrl = propertyService.findStringValue(CmPropKey.STD_WIDGET_IDE_GIT_URL)
//				.orElseThrow(ResourceNotFoundException::new);
//		
//		List<Dependence> result = projectDependenceService
//				.findProjectDependences(project.getId(), true)
//				.stream()
//				.filter(item -> item.getComponentRepo().getRepoType().equals(RepoType.IDE))
//				.map(item -> {
//					Dependence dependence = new Dependence();
//
//					ComponentRepo componentRepo = item.getComponentRepo();
//					dependence.setId(componentRepo.getId());
//					dependence.setGitRepoWebsite(componentRepo.getGitRepoWebsite());
//					dependence.setGitRepoOwner(componentRepo.getGitRepoOwner());
//					dependence.setGitRepoName(componentRepo.getGitRepoName());
//					dependence.setCategory(componentRepo.getCategory().getValue());
//					dependence.setStd(stdIdeWidgetGitUrl.equals(componentRepo.getGitRepoUrl()));
//					dependence.setVersion(item.getComponentRepoVersion().getVersion());
//					dependence.setApiRepoId(item.getApiRepo().getId());
//					
//					return dependence;
//				})
//				.collect(Collectors.toList());
//		return ResponseEntity.ok(result);
	}
	
	/**
	 * 获取项目依赖的 API 组件库中类型为 Widget 的组件库中的所有部件。
	 * 并按照组件库和部件种类分组。
	 * 
	 * @return
	 */
	@GetMapping("/designer/projects/{projectId}/dependences/widgets")
	public ResponseEntity<List<RepoWidgetList>> getProjectDependenceWidgets(
			Principal principal,
			@PathVariable Integer projectId) {
		Repository project = repositoryService.findById(projectId).orElseThrow(ResourceNotFoundException::new);
		repositoryPermissionService.canRead(principal, project).orElseThrow(NoAuthorizationException::new);
		
		List<RepoWidgetList> result = projectDependenceService.findAllWidgets(project.getId());
		return ResponseEntity.ok(result);
	}
	
	@GetMapping("/designer/pages/{pageId}/model")
	public ResponseEntity<PageModel> getPageModel(
			Principal principal, 
			@PathVariable Integer pageId) {
		RepositoryResource page = repositoryResourceService.findById(pageId).orElseThrow(ResourceNotFoundException::new);
		if(!page.isPage()) {
			throw new ResourceNotFoundException();
		}
		
		// TODO: 重新设计项目依赖
		Repository repository = repositoryService.findById(page.getRepositoryId()).orElseThrow(ResourceNotFoundException::new);
		repositoryPermissionService.canRead(principal, repository).orElseThrow(NoAuthorizationException::new);
		
		PageModel result = repositoryResourceService.getPageModel(repository.getId(), page);
		return ResponseEntity.ok(result);
	}
	
	@PutMapping("/designer/pages/{pageId}/model")
	public ResponseEntity<Map<String, Object>> updatePageModel(
			Principal principal, 
			@PathVariable Integer pageId, 
			@RequestBody PageModel model ) {
		// ensure user login
		if(principal == null) {
			throw new NoAuthorizationException();
		}

		RepositoryResource page = repositoryResourceService.findById(pageId).orElseThrow(ResourceNotFoundException::new);
		if(!(page.isPage())) {
			throw new ResourceNotFoundException();
		}
		Repository project = repositoryService.findById(page.getRepositoryId()).orElseThrow(ResourceNotFoundException::new);
		repositoryPermissionService.canWrite(principal, project).orElseThrow(NoAuthorizationException::new);
		
		repositoryResourceService.updatePageModel(project, page, model);
		
		return ResponseEntity.noContent().build();
	}
	
	private static final String[] VALID_ASSET_NAMES = {"main.bundle.js", "main.bundle.js.map", "main.bundle.css", "main.bundle.css.map", "icons.svg"};
	@GetMapping("/designer/assets/{gitRepoWebsite}/{gitRepoOwner}/{gitRepoName}/{version}/{fileName}")
	public ResponseEntity<InputStreamSource> getAsset(
			@PathVariable String gitRepoWebsite,
			@PathVariable String gitRepoOwner,
			@PathVariable String gitRepoName,
			@PathVariable String version,
			@PathVariable String fileName) {
		
		Arrays.stream(VALID_ASSET_NAMES).filter(item -> item.equals(fileName)).findAny().orElseThrow(ResourceNotFoundException::new);
		
		String dataRootPath = propertyService.findStringValue(CmPropKey.BLOCKLANG_ROOT_PATH, "");
		MarketplaceStore store = new MarketplaceStore(dataRootPath, gitRepoWebsite, gitRepoOwner, gitRepoName);
		Path filePath = store.getPackageVersionDirectory(version).resolve(fileName);
		
		if(Files.notExists(filePath)) {
			throw new ResourceNotFoundException();
		}
		
		try {
			InputStream io = ResourceUtils.getURL(ResourceUtils.FILE_URL_PREFIX + filePath.toString()).openStream();
			InputStreamResource resource = new InputStreamResource(io);
			
			MediaType contentType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
			return ResponseEntity.ok().contentType(contentType).body(resource);
		} catch (IOException e) {
			throw new ResourceNotFoundException();
		}
	}
}
