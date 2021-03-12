package com.shengsiyuan.spring.lecture.dynamicproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * DynamicProxyClient
 *
 * @author zhucj
 * @since 20210325
 */
public class DynamicProxyClient {

	public static void main(String[] args) {
		InvocationHandler invocationHandler = new DynamicProxySubject(new RealSubject());

		Object proxyInstance = Proxy.newProxyInstance(
				invocationHandler.getClass().getClassLoader(),
				RealSubject.class.getInterfaces(),
				invocationHandler
		);

		System.out.println(proxyInstance.getClass());
		Subject subject = (Subject) proxyInstance;

		System.out.println("******************");

		subject.myRequest();
	}
}