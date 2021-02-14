package com.javaxxl.lookup;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * LookUpTtest
 *
 * @author zhucj
 * @since 20210225
 */
public class LookUpTest {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-look-up.xml");
		GetBeanTest getBeanTest = (GetBeanTest) context.getBean("getBeanTest");
		//其实是根据动态代理实现
		getBeanTest.showMe();
	}
}
