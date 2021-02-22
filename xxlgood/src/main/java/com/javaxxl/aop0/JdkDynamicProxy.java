package com.javaxxl.aop0;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JdkDynamicProxy
 *
 * @author zhucj
 * @since 20210225
 */
public class JdkDynamicProxy implements InvocationHandler {

	/**
	 * 被代理对象
	 */
	private Object target;

	public JdkDynamicProxy(Object target) {
		this.target = target;
	}

	/**
	 * @param proxy 代理对象，代理了target.代理对象内部持有target对象
	 * @param method 被代理对象的方法
	 * @param args 对代理对象方法的入参数
	 * @return 结果
	 * @throws Throwable 异常
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		//需求：打印动物吃饭开始时间跟结束时间

		System.out.println("开始" + System.currentTimeMillis());
		Object invoke = method.invoke(target, args);
		System.out.println("结束" + System.currentTimeMillis());
		return invoke;
	}

	/**
	 * 返回代理之后的对象
	 *
	 * @return 代理之后的对象
	 */
	public Object getProxy() {
		return Proxy.newProxyInstance(
				target.getClass().getClassLoader(),//类加载起
				target.getClass().getInterfaces(), //代理类需要实现的接口集合
				this); // 代理类虽然全部实现类接口方法，但是接口方法要依靠InvocationHandler去处理
	}
}
