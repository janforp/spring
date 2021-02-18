package com.javaxxl.propertyeditor;

import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * PropertyEditorMain
 *
 * @author zhucj
 * @since 20210225
 */
public class PropertyEditorMain {

	/**
	 * @see DatePropertyEditorRegistrar
	 * @see CustomEditorConfigurer#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-property-editor.xml");
		Student student = (Student) context.getBean("student");
		System.out.println(student.getBirthday());
	}
}