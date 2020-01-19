package com.taotao.sso.service.imp;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.taotao.common.pojo.TaotaoResult;
import com.taotao.common.util.JsonUtils;
import com.taotao.mapper.TbUserMapper;
import com.taotao.pojo.TbUser;
import com.taotao.pojo.TbUserExample;
import com.taotao.pojo.TbUserExample.Criteria;
import com.taotao.sso.jedis.JedisClient;
import com.taotao.sso.service.UserLoginService;

/**
 * 用户登录的实现
 * @author aa
 *
 */
@Service
public class UserLoginServiceImp implements UserLoginService{

	@Autowired
	private TbUserMapper mapper;
	@Autowired
	private JedisClient client;
	
	@Value("${USER_INFO}")
	private String USER_INFO;
	@Value("${EXPIRE_TIME}")
	private Integer EXPIRE_TIME;
	
	@Override
	public TaotaoResult login(String username, String password) {
//		1. 注入mapper 
//		2. 校验用户名和密码是否为空
		if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)){
			return TaotaoResult.build(400, "用户名或密码不可为空");
		}
//		3. 先校验用户名
		TbUserExample example = new TbUserExample();
		Criteria criteria = example.createCriteria();
		criteria.andUsernameEqualTo(username);
		List<TbUser> list = mapper.selectByExample(example);
		if(list == null || list.size()==0){
			return TaotaoResult.build(400, "用户名不存在");
		}
//		4. 再校验密码
		TbUser user = list.get(0);
//		先加密密码在比较
		String md5password = DigestUtils.md5DigestAsHex(password.getBytes());
		if(!md5password.equals(user.getPassword())){
			return TaotaoResult.build(400, "密码错误");
		}
//		5. 如果校验成功
//		6. 生成token:uuid生成
		String token = UUID.randomUUID().toString();
//		存放用户数据到redis中,使用jedis
//		为了更好的安全性,我们设置密码为空再插入
		user.setPassword(null);
		client.set(USER_INFO+":"+token, JsonUtils.objectToJson(user));
		client.expire(USER_INFO+":"+token, EXPIRE_TIME);
//		7. 把token设置cookie中  在表现层设置
		return TaotaoResult.ok(token);
	}

	@Override
	public TaotaoResult getUserByToken(String token) {
//		1. 注入jedisclient
//		2. 根据token查询 用户信息的方法
		String strjson = client.get(USER_INFO+":"+token);
//		3. 判断是否能够查询到
//		查询到,返回200,并且包含用户信息
		if(StringUtils.isNotBlank(strjson)){
			TbUser tbUser = JsonUtils.jsonToPojo(strjson, TbUser.class);
//			重新设置过期时间
			client.expire(USER_INFO+":"+token, EXPIRE_TIME);
			return TaotaoResult.ok(tbUser);
		}
//		查询不到,返回400
		return TaotaoResult.build(400, "用户信息已过期,请重新登录");
	}

	@Override
	public TaotaoResult logout(String token) {
		client.expire(USER_INFO+":"+token, 0);
		return TaotaoResult.ok();
	}
}
