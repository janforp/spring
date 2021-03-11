package com.shengsiyuan.spring.lecture.aop.advisor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * MyAdvisor
 *
 * @author zhucj
 * @since 20210325
 */
public class MyAdvisor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		System.out.println("before 方法调用");
		//其实就是调用拦截器链中的下一个
		Object result = invocation.proceed();
		System.out.println("after 方法调用");
		return result;
	}
}