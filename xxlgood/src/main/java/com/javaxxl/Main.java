package com.javaxxl;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Main
 *
 * @author zhucj
 * @since 20210225
 */
public class Main {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-test.xml");
		UserService userService = (UserService) context.getBean("userService");
		User user = userService.getUserById(1);
		System.out.println("******************************************************************************************");
		System.out.println(user);
		System.out.println("******************************************************************************************");
	}
}