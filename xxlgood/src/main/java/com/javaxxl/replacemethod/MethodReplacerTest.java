package com.javaxxl.replacemethod;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * MethodReplacerTest
 *
 * @author zhucj
 * @since 20210225
 */
public class MethodReplacerTest {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-method-replace.xml");
		TestChangeMethod testChangeMethod = (TestChangeMethod) context.getBean("testChangeMethod");
		//其实是根据动态代理实现
		testChangeMethod.changeMe();
	}
}
