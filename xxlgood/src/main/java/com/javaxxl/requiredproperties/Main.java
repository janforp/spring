package com.javaxxl.requiredproperties;

import org.springframework.context.ApplicationContext;

/**
 * Main
 *
 * @author zhucj
 * @since 20210225
 */
public class Main {

	public static void main(String[] args) {
		ApplicationContext context = new MyClassPathXmlApplicationContext("spring-required-properties-test.xml");
	}
}
