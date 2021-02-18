package com.javaxxl.beanFactoryBeanPostProcessor;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * BeanFactoryBeanPostProcessorMain
 *
 * @author zhucj
 * @since 20210225
 */
public class BeanFactoryBeanPostProcessorMain {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-bfbppr.xml");
		Book book01 = (Book) context.getBean("book-01");
		Book book02 = (Book) context.getBean("book-02");
		Book book03 = (Book) context.getBean("book-03");

		System.out.println(book01);
		System.out.println(book02);
		System.out.println(book03);
	}
}