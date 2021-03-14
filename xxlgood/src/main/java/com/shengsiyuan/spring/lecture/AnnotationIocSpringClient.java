package com.shengsiyuan.spring.lecture;

import com.shengsiyuan.spring.lecture.annotation.Person;
import com.shengsiyuan.spring.lecture.annotation.PersonConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * AnnotationSpringClient
 *
 * @author zhucj
 * @since 20210325
 */
public class AnnotationIocSpringClient {

	public static void main(String[] args) {

		/**
		 * 基于注解的ioc工厂
		 * 对应基于 xml的步骤为：
		 * DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		 */
		AnnotationConfigApplicationContext annotationIoc
				= new AnnotationConfigApplicationContext();

		/**
		 * 注册
		 *
		 * Resource resource = new ClassPathResource("spring-construct-property.xml");
		 * XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		 * reader.loadBeanDefinitions(resource);
		 */
		annotationIoc.register(PersonConfiguration.class);

		//刷新
		annotationIoc.refresh();

		Person person = (Person) annotationIoc.getBean("person");
		System.out.println(person.getId() + " 名称为 " + person.getName());
	}
}