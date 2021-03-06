package com.blocklang.marketplace.apirepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.springframework.util.Assert;

import com.blocklang.core.git.GitBlobInfo;
import com.blocklang.core.git.GitUtils;
import com.blocklang.core.runner.common.CliLogger;
import com.blocklang.core.runner.common.JsonSchemaValidator;
import com.blocklang.core.util.JsonUtil;
import com.blocklang.marketplace.apirepo.apiobject.ApiObjectContext;
import com.blocklang.marketplace.data.MarketplaceStore;

public abstract class RefParser {
	/**
	 * tag 或 master 分支的全称，如 "refs/tags/v0.1.0"
	 */
	protected String fullRefName;
	
	/**
	 * ref 的简称，如 refs/tags/v0.1.0 简称为 0.1.0；refs/heads/master 简称为 master
	 */
	protected String shortRefName;

	protected MarketplaceStore store;
	protected CliLogger logger;
	
	protected List<String> tags;
	
	private ApiObjectContext apiObjectContext;
	private ChangeParserFactory changeParserFactory;
	private JsonSchemaValidator schemaValidator;
	
	ApiRepoPathReader pathReader = new ApiRepoPathReader();

	// 按照 Api Object 分组
	private LinkedHashMap<String, List<GitBlobInfo>> allApiObjectChangelogFiles;
	
	public RefParser(List<String> tags, MarketplaceStore store, CliLogger logger) {
		this.tags = tags;
		this.store = store;
		this.logger = logger;
	}
	
	public void setApiObjectContext(ApiObjectContext context) {
		// 注意：
		// apiObjectContext 是 ref 级别的变量，在不同 ref 间不能共享
		// 每次运行都是清空其中的数据
		this.apiObjectContext = context;
		this.apiObjectContext.setTags(tags);
	}
	
	public void setChangeParserFactory(ChangeParserFactory factory) {
		this.changeParserFactory = factory;
	}
	
	public void setSchemaValidator(JsonSchemaValidator schemaValidator) {
		this.schemaValidator = schemaValidator;
	}

	public void setFullRefName(String fullRefName) {
		this.fullRefName = fullRefName;
	}

	/**
	 * 如果是 tag，则解析出语义版本号；如果是 master 分支，则值为 master
	 * 
	 * @param shortRefName
	 */
	public void setShortRefName(String shortRefName) {
		this.shortRefName = shortRefName;
	}

	public ParseResult run() {
		return ParseResult.SUCCESS;
	}
	
	protected void setup() {
		Assert.notNull(fullRefName, "请先先择 tag");
		Assert.notNull(shortRefName, "请传入 tag 对应的版本号");
		Assert.notNull(apiObjectContext, "请传入 apiObjectContext 对象");
		
		this.apiObjectContext.init(fullRefName, shortRefName);
	}
	
	/**
	 * 读取某 tag 或 master 分支下所有 changelog 文件，并按照 API Object 分组。
	 * 并将结果存到 {@link #allApiObjectChangelogFiles} 变量下
	 * 
	 * 读取 changelog/ 下除 schemas 目录外的所有 json 文件。
	 */
	protected void readAllChangelogFiles() {
		PathSuffixFilter pathSuffixFilter = PathSuffixFilter.create(".json");
		PathFilter pathFilter = PathFilter.create("changelog");
		TreeFilter excludeFilter = PathFilter.create("changelog/schemas").negate();
		TreeFilter filter = AndTreeFilter.create(Arrays.asList(pathFilter, excludeFilter, pathSuffixFilter));
		
		this.allApiObjectChangelogFiles = GitUtils
			.readAllFileContent(store.getRepoSourceDirectory(), fullRefName, filter)
			.stream()
			.collect(Collectors.groupingBy(
				// path 中 0 是 changelog，1 是 分组名
				fileInfo -> fileInfo.getPath().split("/")[1], 
				LinkedHashMap::new, 
				Collectors.toList()));
	}

	/**
	 * 在某 tag 或 master 分支下是否包含 changelog 文件
	 * 
	 * @return 如果包含 changelog 文件则返回 <code>true</code>，否则返回 <code>false</code>
	 */
	protected boolean notFoundAnyChangelogFiles() {
		if(this.allApiObjectChangelogFiles.isEmpty()) {
			return true;
		}
		
		return this.allApiObjectChangelogFiles
				.entrySet()
				.stream()
				.allMatch(entry -> entry.getValue().isEmpty());
	}

	protected boolean validateDirAndFileName() {
		var validator = new RefChangelogNameValidator(logger);
		return validator.isValid(allApiObjectChangelogFiles);
	}
	
	protected boolean validateJsonSchema() {
		var validator = new RefChangelogSchemaValidator(logger, schemaValidator);
		return validator.isValid(allApiObjectChangelogFiles);
	}
	
	/**
	 * 如果 changelog 文件已发布过，即已标注过 tag，则不允许修改此 changelog 文件。
	 * 此方法使用 MD5 校验文件的内容是否发生了变化。
	 * 
	 * 如果文件未发布过，则在 master 分支可任意修改。
	 * 
	 * 此方法也是校验所有发布过的 changelog 文件，不是遇到一处错误，就不再校验。
	 * 
	 * @return 如果有文件被修改，则返回 <code>true</code>；如果所有文件都没有更新则返回 <code>false</code>
	 */
	protected boolean publishedChangelogFileUpdated() {
		boolean hasPublishedChangeLogUpdated = false;
		
		// 逐个 API Object 校验
		for(Map.Entry<String, List<GitBlobInfo>> entry : allApiObjectChangelogFiles.entrySet()) {
			String directoryName = entry.getKey();
			List<GitBlobInfo> changelogFiles = entry.getValue();
			
			// 注意，在每次获取已发布 change 时都要设置 apiObjectId
			String apiObjectId = pathReader.read(directoryName).getOrder();
			apiObjectContext.setObjectId(apiObjectId);
			List<PublishedFileInfo> publishedFiles = getPublishedFiles(apiObjectId);

			// 校验已发布的文件是否被修改过
			for (GitBlobInfo jsonFile : changelogFiles) {
				String jsonFileId = pathReader.read(jsonFile.getName()).getOrder();
				// 判断该文件是否已执行过
				Optional<PublishedFileInfo> changelogInfoOption = publishedFiles
					.stream()
					.filter(changelog -> changelog.getFileId().equals(jsonFileId))
					.findFirst();

				if (changelogInfoOption.isPresent()) {
					// 如果已执行过，但文件内容已修改，则给出错误提示
					String md5sumPublished = changelogInfoOption.get().getMd5sum();
					String md5sumNow = DigestUtils.md5Hex(jsonFile.getContent());
					if (!md5sumPublished.equals(md5sumNow)) {
						logger.error("{0}/{1} 已被修改，已应用版本的 checksum 为 {2}，但当前版本的 checksum 为 {3}", 
							directoryName,
							jsonFile.getName(), 
							md5sumPublished, 
							md5sumNow);
						hasPublishedChangeLogUpdated = true;
					}
				}
			}

			// 如果有一个 apiObject 校验失败，则就不需要再保存后续的信息
			if(!hasPublishedChangeLogUpdated) {
				// 只缓存校验成功后的 ApiObject 的已发布文件信息
				apiObjectContext.addPublishedChangelogFiles(publishedFiles);
			}
		}
		return hasPublishedChangeLogUpdated;
	}

	protected List<PublishedFileInfo> getPublishedFiles(String apiObjectId) {
		List<PublishedFileInfo> result = new ArrayList<>();
		Path changelogPath = store.getPackageChangelogDirectory().resolve(apiObjectId).resolve("index.json");
		try {
			String changelogJson = Files.readString(changelogPath);
			List<PublishedFileInfo> published = JsonUtil.fromJsonArray(changelogJson, PublishedFileInfo.class);
			
			// 因为 master 分支每次都要重新解析，所以清除 master 分支的解析记录
			published.removeIf(fileInfo -> fileInfo.getVersion().equals("master"));
			result.addAll(published);
		} catch (IOException e1) {
			// 如果文件不存在，则使用空 List
		}
		return result;
	}

	protected boolean parseAllApiObject() {
		boolean success = true;
		var apiObjectParser = new ChangedObjectParser(changeParserFactory);
		
		for (Map.Entry<String, List<GitBlobInfo>> entry : allApiObjectChangelogFiles.entrySet()) {
			List<GitBlobInfo> changelogFiles = entry.getValue();
			String apiObjectId = pathReader.read(entry.getKey()).getOrder();
			// 注意，在每次解析时都要设置当前的 apiObjectId
			apiObjectContext.setObjectId(apiObjectId);
			
			apiObjectContext.loadPreviousVersionObject();
			
			success = apiObjectParser.run(apiObjectContext, changelogFiles);
		}
		return success;
	}

	protected boolean saveAllApiObject() {
		return apiObjectContext.saveAllChangedObjects(shortRefName);
	}
}
