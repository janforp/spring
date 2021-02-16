package com.javaxxl.bpp;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * BppMain
 *
 * @author zhucj
 * @since 20210225
 */
public class BppMain {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring-bpp-instantiation.xml");
		Student student = context.getBean(Student.class);
		System.out.println(student);
	}
}
