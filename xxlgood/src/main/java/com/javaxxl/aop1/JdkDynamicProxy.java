package com.javaxxl.aop1;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JdkDynamicProxy
 *
 * @author zhucj
 * @since 20210225
 */
public class JdkDynamicProxy implements InvocationHandler {

	/**
	 * 被代理对象,调用该对象的任何方法都会被重定向到方法{@link JdkDynamicProxy#invoke(Object, Method, Object[])}
	 */
	private final Object target;

	/**
	 * 拦截器列表，这些拦截器就是为了增强 被代理对象的
	 */
	private final List<MyMethodInterceptor> interceptorList;

	/**
	 * 创建JdkDynamicProxy,用来创建代理对象,以及添加拦截器
	 *
	 * @param target 被代理对象
	 * @param interceptorList 拦截器列表，这些拦截器就是为了增强 被代理对象的
	 */
	public JdkDynamicProxy(Object target, List<MyMethodInterceptor> interceptorList) {
		this.target = target;
		this.interceptorList = interceptorList;
	}

	/**
	 * 调用被代理对象的任何方法都会被重定向到该方法
	 *
	 * @param proxy 代理对象
	 * @param method 被代理对象的方法
	 * @param args 被代理对象的方法的参数
	 * @return 被代理对象的方法的返回值
	 * @throws Throwable 异常
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (!method.getName().contains("eat")) {
			return method.invoke(target, args);
		}
		//封装被代理对象的方法
		TargetMethod targetMethod = new TargetMethod(target, method, args);
		//拦截器驱动
		MyMethodInvocation invocation = new MyMethodInvocationImpl(targetMethod, interceptorList);
		//先这些拦截器，再调用被代理对象的方法
		return invocation.proceed();//驱动拦截器按顺序执行，最后调用被代理方法
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
