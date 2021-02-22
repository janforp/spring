package com.javaxxl.aop2;

import com.javaxxl.aop0.Animal;
import com.javaxxl.aop0.Cat;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.lang.NonNull;

/**
 * Main
 *
 * @author zhucj
 * @since 20210225
 */
public class Main {

	public static void main(String[] args) {
		//1.创建被代理对象
		Cat cat = new Cat();

		//2.创建spring代理工厂对象 ProxyFactory
		//ProxyFactory 是 config + factory 的存在，持有aop操作所有的生产资料
		ProxyFactory proxyFactory = new ProxyFactory(cat);

		//3.添加方法拦截器
		proxyFactory.addAdvice(new MethodInterceptor01());
		proxyFactory.addAdvice(new MethodInterceptor02());

		//4.获取代理对象
		Animal proxy = (Animal) proxyFactory.getProxy();

		proxy.eat();
	}

	private static class MethodInterceptor01 implements MethodInterceptor {

		@Override
		public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
			System.out.println("拦截器1 开始");
			Object proceed = invocation.proceed();
			System.out.println("拦截器1 结束");
			return proceed;
		}
	}

	private static class MethodInterceptor02 implements MethodInterceptor {

		@Override
		public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
			System.out.println("拦截器2 开始");
			Object proceed = invocation.proceed();
			System.out.println("拦截器2 结束");
			return proceed;
		}
	}
}
