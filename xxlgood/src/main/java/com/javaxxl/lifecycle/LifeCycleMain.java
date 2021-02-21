package com.javaxxl.lifecycle;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * LifeCycleMain
 *
 * @author zhucj
 * @since 20210225
 */
public class LifeCycleMain {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-test-life-cycle.xml");
		//		context.start();
		//		context.close();
	}
}