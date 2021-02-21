package com.janita.proxy.aop2;

import com.janita.proxy.aop1.Animal;
import com.janita.proxy.aop1.Cat;
import com.janita.proxy.aop1.JdkDynamicProxy;

/**
 * TestMain
 *
 * @author zhucj
 * @since 20210225
 */
public class Test2Main {

	public static void main(String[] args) {
		//1.创建被代理对象
		Cat cat = new Cat();
		JdkDynamicProxy proxy1 = new JdkDynamicProxy(cat);

		//第一层代理
		Animal proxyAnimal1 = (Animal) proxy1.getProxy();

		JdkDynamicProxy2 proxy2 = new JdkDynamicProxy2(proxyAnimal1);

		//第二层代理
		Animal proxyAnimal2 = (Animal) proxy2.getProxy();

		proxyAnimal2.eat();
	}
}