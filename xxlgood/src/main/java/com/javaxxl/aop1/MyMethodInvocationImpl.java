package com.javaxxl.aop1;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * MyMethodInvocationImpl
 *
 * @author zhucj
 * @since 20210225
 */
public class MyMethodInvocationImpl implements MyMethodInvocation {

	/**
	 * 封装了被代理方法，拦截器都执行完成之后就要调用该方法
	 */
	private final TargetMethod targetMethod;

	/**
	 * 拦截器链表
	 */
	private final List<MyMethodInterceptor> interceptorList;

	/**
	 * 拦截器执行的次序
	 */
	private int index = 0;

	public MyMethodInvocationImpl(TargetMethod targetMethod, List<MyMethodInterceptor> interceptorList) {
		this.targetMethod = targetMethod;
		this.interceptorList = interceptorList;
	}

	@Override
	public Object proceed() throws InvocationTargetException, IllegalAccessException {
		/**
		 * 先执行拦截器，然后再执行被代理对象的方法
		 * 通过 index 下标自增的方式进行
		 * 因为 当前对象 是在一个代理对象中是单实例的！interceptorList 实例就一个，只是在不断的往下传
		 */

		if (index == interceptorList.size()) {
			//所有拦截器都执行完毕，则执行代理对象自己，通过代理方法反射调用！！！
			return targetMethod.getMethod().invoke(
					targetMethod.getTarget(),
					targetMethod.getArgs());
		}

		//按 index 的顺序执行拦截器
		MyMethodInterceptor interceptor = interceptorList.get(index++);
		return interceptor.invoke(
				/**
				 * 拦截器中传的还是当前对象自己，所以说当前对象是单实例的！！！
				 */
				this
		);
	}
}
