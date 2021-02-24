package com.javaxxl.aop4aspect;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * AspectMain
 *
 * @author zhucj
 * @since 20210225
 */
public class AspectMain {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-aop-aspect-test.xml");
		TargetInterfaceTest targetInterfaceTest = (TargetInterfaceTest) context.getBean("targetInterfaceTest");

		targetInterfaceTest.doSomeTest();
		System.out.println("---------");
		targetInterfaceTest.doOtherTest();
		System.out.println("---------");
		targetInterfaceTest.a();
		System.out.println("---------");
		targetInterfaceTest.b();
	}
}
