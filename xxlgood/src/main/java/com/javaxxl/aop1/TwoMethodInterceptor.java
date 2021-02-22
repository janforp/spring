package com.javaxxl.aop1;

import java.lang.reflect.InvocationTargetException;

/**
 * TwoMethodInterceptor
 *
 * @author zhucj
 * @since 20210225
 */
public class TwoMethodInterceptor implements MyMethodInterceptor {

	@Override
	public Object invoke(MyMethodInvocation myMethodInvocation) throws InvocationTargetException, IllegalAccessException {
		System.out.println("拦截器2开始");
		Object proceed = myMethodInvocation.proceed();
		System.out.println("拦截器2完成");
		return proceed;
	}
}