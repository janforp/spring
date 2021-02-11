package com.javaxxl;

/**
 * UserServiceImpl
 *
 * @author zhucj
 * @since 20210225
 */
public class UserServiceImpl implements UserService {

	@Override
	public User getUserById(int id) {
		User user = new User();
		user.setAge(17);
		user.setName("小刘");
		return user;
	}
}
