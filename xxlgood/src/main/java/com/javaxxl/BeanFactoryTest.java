package com.javaxxl;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * BeanFactoyTest
 *
 * @author zhucj
 * @since 20210225
 */
public class BeanFactoryTest {

	public static void main(String[] args) {

		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-bf.xml"));
		Object a = beanFactory.getBean("componentA");
		Object b = beanFactory.getBean("componentB");
		System.out.println(a);
		System.out.println(b);
	}
}
