package com.shengsiyuan.spring.lecture.dynamicproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * DynamicProxySubject
 *
 * @author zhucj
 * @since 20210325
 */
public class DynamicProxySubject implements InvocationHandler {

	private final Object realSubject;

	public DynamicProxySubject(Object realSubject) {
		this.realSubject = realSubject;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("方法调用之前");
		Object result = method.invoke(realSubject, args);
		System.out.println(method.getName());
		System.out.println("方法调用之后");
		System.out.println(proxy.getClass());
		return result;
	}
}