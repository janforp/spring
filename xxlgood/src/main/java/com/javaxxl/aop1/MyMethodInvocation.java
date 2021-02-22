package com.javaxxl.aop1;

import java.lang.reflect.InvocationTargetException;

/**
 * MyMethodInvocation
 *
 * @author zhucj
 * @since 20210225
 */
public interface MyMethodInvocation {

	/**
	 * 驱动拦截器练，执行增强拦截器，被代理方法调用
	 *
	 * @return 方法返回
	 * @throws InvocationTargetException 异常
	 * @throws IllegalAccessException 异常
	 */
	Object proceed() throws InvocationTargetException, IllegalAccessException;
}