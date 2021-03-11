package com.javaxxl.aop1;

import com.javaxxl.aop0.Animal;
import com.javaxxl.aop0.Cat;

import java.util.Arrays;

/**
 * Main
 *
 * @author zhucj
 * @since 20210225
 */
public class Main {

	public static void main(String[] args) {
		//创建被代理对象
		Cat cat = new Cat();

		//创建JdkDynamicProxy,用来创建代理对象,以及添加拦截器
		JdkDynamicProxy proxy = new JdkDynamicProxy(
				cat,//被代理对象
				Arrays.asList(new OneMethodInterceptor(), new TwoMethodInterceptor()) // 增强列表
		);

		//获取代理对象
		Animal animal = (Animal) proxy.getProxy();
		//
		/**
		 * 调用方法，其实就会调用 {@link com.javaxxl.aop1.JdkDynamicProxy#invoke} 方法，该方法会驱动拦截器前进
		 */
		animal.eat();
	}
}
