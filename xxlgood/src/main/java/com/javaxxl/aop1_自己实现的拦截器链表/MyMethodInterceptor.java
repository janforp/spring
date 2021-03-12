package com.javaxxl.aop1_自己实现的拦截器链表;

import java.lang.reflect.InvocationTargetException;

/**
 * MyMethodInterceptor
 *
 * @author zhucj
 * @since 20210225
 */
public interface MyMethodInterceptor {

	/**
	 * 方法拦截器接口，增强逻辑全部写在里面
	 */
	Object invoke(MyMethodInvocation myMethodInvocation) throws InvocationTargetException, IllegalAccessException;
}