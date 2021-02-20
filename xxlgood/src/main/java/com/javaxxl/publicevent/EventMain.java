package com.javaxxl.publicevent;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.Executors;

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
		SimpleApplicationEventMulticaster multicaster = context.getBean(SimpleApplicationEventMulticaster.class);
		/**
		 * 如果不提供线程池，则在主线程中执行
		 */
		multicaster.setTaskExecutor(Executors.newFixedThreadPool(2));
		EatEvent xEvent = new EatEvent(true);
		context.publishEvent(xEvent);
		System.out.println("******************************************************************************************");
	}
}