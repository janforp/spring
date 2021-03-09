package com.shengsiyuan.spring.lecture;

import com.javaxxl.UserService;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * SpringClient
 *
 * @author zhucj
 * @since 20210325
 */
public class SpringClient {

	public static void main(String[] args) {
		Resource resource = new ClassPathResource("spring-test.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		reader.loadBeanDefinitions(resource);

		UserService service = (UserService) factory.getBean("userService");
		System.out.println(service);
	}
}