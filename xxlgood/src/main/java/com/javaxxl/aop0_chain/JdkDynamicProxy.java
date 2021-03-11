package com.javaxxl.aop0_chain;

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

	private AbstractHandler headHandler;

	public JdkDynamicProxy(Object target, AbstractHandler headHandler) {
		this.target = target;
		this.headHandler = headHandler;
	}

	/**
	 * @param proxy 代理对象，代理了target.代理对象内部持有target对象
	 * @param method 被代理对象的方法
	 * @param args 对代理对象方法的入参数
	 * @return 结果
	 * @throws Throwable 异常
	 * @see com/janita/java/base/proxy/JdkDynamicProxy.class:32
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		TargetMethod targetMethod = new TargetMethod(target, method, args);
		return headHandler.proceed(targetMethod);
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
