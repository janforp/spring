package com.janita.proxy.aop2;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JdkDynamicProxy2
 *
 * @author zhucj
 * @since 20210225
 */
public class JdkDynamicProxy2 implements InvocationHandler {

	/**
	 * 被代理对象
	 */
	private Object target;

	public JdkDynamicProxy2(Object target) {
		this.target = target;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("方法的参数个数为:" + (args != null ? args.length : 0));
		Object invoke = method.invoke(target, args);
		System.out.println("方法的返回值 ：" + (invoke != null ? invoke : "空"));
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
