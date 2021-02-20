package com.javaxxl.publicevent;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * EventMain
 *
 * @author zhucj
 * @since 20210225
 */
public class EventMain {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-test-event-listener.xml");
		System.out.println("******************************************************************************************");
		EatEvent xEvent = new EatEvent(true);
		context.publishEvent(xEvent);
		System.out.println("******************************************************************************************");
	}
}