package com.blocklang.core.service.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.blocklang.core.constant.DataType;
import com.blocklang.core.dao.PropertyDao;
import com.blocklang.core.model.CmProperty;
import com.blocklang.core.service.PropertyService;
import com.blocklang.core.test.AbstractServiceTest;

public class PropertyServiceImplTest extends AbstractServiceTest{
	
	// 因为 CM_PROPERTY 表中默认是有值的，测试用例的数据不能使用已有值的 id，
	// 所以这里使用int 类型的最大值
	private Integer MAX_ID = Integer.MAX_VALUE;

	@Autowired
	private PropertyService propertyService;
	
	@Autowired
	private PropertyDao propertyDao;
	
	@Autowired
	private CacheManager cacheManager;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@After
	public void tearDown() {
		// 为了避免缓存干扰其他测试用例，每次执行后都清空缓存
		this.cacheManager.getCache("cm_properties").clear();
	}
	
	@Test
	public void find_string_value_no_data() {
		Optional<String> valueOption = propertyService.findStringValue("not-exist-key");
		assertThat(valueOption.isEmpty(), is(true));
	}
	
	@Test
	public void find_string_value_is_invalid() {
		CmProperty property = new CmProperty();
		property.setId(MAX_ID);
		property.setKey("key");
		property.setValue("value");
		property.setValid(false);
		property.setDataType(DataType.STRING);
		propertyDao.save(property);
		
		Optional<String> valueOption = propertyService.findStringValue("key");
		assertThat(valueOption.isEmpty(), is(true));
	}
	
	@Test
	public void find_string_value_set_wrong_parent_id() {
		CmProperty property = new CmProperty();
		property.setId(MAX_ID);
		property.setKey("key");
		property.setValue("value");
		property.setValid(true);
		property.setDataType(DataType.STRING);
		property.setParentId(-2); // not -1
		propertyDao.save(property);
		
		Optional<String> valueOption = propertyService.findStringValue("key");
		assertThat(valueOption.isEmpty(), is(true));
	}

	@Test
	public void find_string_value_not_set_parent_id() {
		thrown.expect(DataIntegrityViolationException.class);
		
		CmProperty property = new CmProperty();
		property.setId(MAX_ID);
		property.setKey("key");
		property.setValue("value");
		property.setValid(true);
		property.setDataType(DataType.STRING);
		property.setParentId(null); // 不能设置为 null
		// 如果使用 save 则不会做真正的保存，所以不会抛异常
		// 这里需要立即生效，所以加上 flush 操作
		propertyDao.saveAndFlush(property);
	}
	
	@Test
	public void find_string_value_success() {
		CmProperty property = new CmProperty();
		property.setId(MAX_ID);
		property.setKey("key");
		property.setValue("value");
		property.setDataType(DataType.STRING);
		propertyDao.save(property);
		
		Optional<String> valueOption = propertyService.findStringValue("key");
		assertThat(valueOption.get(), equalTo("value"));
	}
	
	@Test
	public void find_string_value_from_cache() {
		CmProperty property = new CmProperty();
		property.setId(MAX_ID);
		property.setKey("key1");
		property.setValue("value");
		property.setDataType(DataType.STRING);
		propertyDao.save(property);
		
		Cache cachedProperties = this.cacheManager.getCache("cm_properties");
		
		Optional<String> valueOption = propertyService.findStringValue("key1");
		assertThat(cachedProperties.get("key1").get(), equalTo(valueOption.get()));
	}
	
}