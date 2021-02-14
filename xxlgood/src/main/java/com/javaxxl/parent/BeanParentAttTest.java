package com.javaxxl.parent;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * BeanParentAttTest
 *
 * @author zhucj
 * @since 20210225
 */
public class BeanParentAttTest {

	public static void main(String[] args) {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-bean-parent.xml"));
		Child child = (Child) beanFactory.getBean("child");
		System.out.println(child.getName());
		System.out.println(child.getAge());
	}
}
