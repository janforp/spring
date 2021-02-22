package com.javaxxl.aop3;

import com.javaxxl.aop0.Animal;
import com.javaxxl.aop0.Cat;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;

/**
 * 只增强 {@link Animal} 的 eat 方法
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

		//3.添加方法拦截器以及指定切点
		MyPointCut pointCut = new MyPointCut();
		proxyFactory.addAdvisor(
				new DefaultPointcutAdvisor(pointCut, new MethodInterceptor01())
		);
		proxyFactory.addAdvisor(
				new DefaultPointcutAdvisor(pointCut, new MethodInterceptor02())
		);

		//4.获取代理对象~
		Animal proxy = (Animal) proxyFactory.getProxy();

		proxy.eat();

		System.out.println("------------------");

		proxy.go();
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

	private static class MyPointCut implements Pointcut {

		@Override
		public ClassFilter getClassFilter() {
			return clazz -> true;
		}

		@Override
		public MethodMatcher getMethodMatcher() {
			return new MethodMatcher() {
				@Override
				public boolean matches(Method method, Class<?> targetClass) {
					return method.getName().equals("eat");
				}

				@Override
				public boolean isRuntime() {
					return false;
				}

				@Override
				public boolean matches(Method method, Class<?> targetClass, Object... args) {
					return false;
				}
			};
		}
	}
}
