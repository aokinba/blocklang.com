package com.blocklang.core.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.blocklang.core.constant.OauthSite;
import com.blocklang.core.dao.UserBindDao;
import com.blocklang.core.model.UserAvatar;
import com.blocklang.core.model.UserBind;
import com.blocklang.core.model.UserInfo;
import com.blocklang.core.service.UserService;

/**
 * 之前考虑将登录相关的共用方法都提取成接口，但是这样就暴露出很多外部并不需要的内部接口，所以决定将内部接口移到抽象类中。
 * 
 * @author Zhengwei Jin
 *
 */
public abstract class AbstractLoginService {

	@Autowired
	protected UserService userService;
	
	@Autowired
	protected UserBindDao userBindDao;
	
	public UserInfo updateUser(OAuth2AccessToken accessToken, OAuth2User oauthUser) {
		String openId = oauthUser.getName();
		Map<String, Object> userAttributes = oauthUser.getAttributes();
		
		UserInfo userInfo = prepareUser(userAttributes);
		List<UserAvatar> userAvatars = prepareUserAvatars(userAttributes);
		UserBind userBind = prepareUserBind(openId);
		
		Optional<UserBind> userBindOption = userBindDao.findBySiteAndOpenId(getOauthSite(), openId);
		if(userBindOption.isEmpty()) {
			return userService.create(userInfo, userBind, userAvatars);
		} else {
			Integer savedUserId = userBindOption.get().getUserId();
			return userService.update(savedUserId, userInfo, userAvatars);
		}
	}
	
	public UserBind prepareUserBind(String openId) {
		UserBind userBind = new UserBind();
		userBind.setSite(getOauthSite());
		userBind.setOpenId(Objects.toString(openId, null));
		userBind.setCreateTime(LocalDateTime.now());
		return userBind;
	}
	
	protected abstract UserInfo prepareUser(Map<String, Object> thirdPartyUser);
	protected abstract List<UserAvatar> prepareUserAvatars(Map<String, Object> thirdPartyUser);
	
	
	protected abstract String getSmallAvatarUrl(String avatarUrl);
	protected abstract String getMediumAvatarUrl(String avatarUrl);
	protected abstract String getLargeAvatarUrl(String avatarUrl);
	
	protected abstract OauthSite getOauthSite();
	
}
