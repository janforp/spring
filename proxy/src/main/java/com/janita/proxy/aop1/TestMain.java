package com.janita.proxy.aop1;

import java.lang.reflect.Proxy;

/**
 * TestMain
 *
 * @author zhucj
 * @since 20210225
 */
public class TestMain {

	public static void main(String[] args) {
		//1.创建被代理对象
		Cat cat = new Cat();
		JdkDynamicProxy proxy = new JdkDynamicProxy(cat);
		//Exception in thread "main" java.lang.ClassCastException: class com.sun.proxy.$Proxy0 cannot be cast to class com.javaxxl.aop1.Cat (com.sun.proxy.$Proxy0 and com.javaxxl.aop1.Cat are in unnamed module of loader 'app')
		//	at com.javaxxl.aop1.TestMain.main(TestMain.java:15)
		// Cat proxyObject = (Cat) proxy.getProxy();

		/**
		 * 代理对象无法转换为 Cat，只能转换为接口类型
		 * @see Proxy#newProxyInstance(ClassLoader, Class[], java.lang.reflect.InvocationHandler)
		 */
		Animal animal = (Animal) proxy.getProxy();
		animal.eat();

		ProxyUtils.generateClassFile(animal.getClass(), proxy.getClass().getSimpleName());
	}
}