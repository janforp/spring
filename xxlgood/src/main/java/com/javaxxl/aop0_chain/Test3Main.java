package com.javaxxl.aop0_chain;

import com.javaxxl.aop0.Animal;
import com.javaxxl.aop0.Cat;

import java.lang.reflect.InvocationTargetException;

/**
 * Test3Main
 *
 * @author zhucj
 * @since 20210225
 */
public class Test3Main {

	public static void main(String[] args) {
		Cat cat = new Cat();

		//head --> oneHandler --> twoHandler
		AbstractHandler headHandler = new AbstractHandler.HeadHandler();
		OneHandler oneHandler = new OneHandler();
		headHandler.setNextHandler(oneHandler);
		oneHandler.setNextHandler(new TwoHandler());

		JdkDynamicProxy jdkDynamicProxy = new JdkDynamicProxy(cat, headHandler);

		Animal proxy = (Animal) jdkDynamicProxy.getProxy();

		proxy.eat();
	}

	private static class OneHandler extends AbstractHandler {

		@Override
		Object invoke(TargetMethod targetMethod) throws InvocationTargetException, IllegalAccessException {
			System.out.println("one handler begin");
			Object ret = proceed(targetMethod);
			System.out.println("one handler end");
			return ret;
		}
	}

	private static class TwoHandler extends AbstractHandler {

		@Override
		Object invoke(TargetMethod targetMethod) throws InvocationTargetException, IllegalAccessException {
			System.out.println("two handler begin");
			Object ret = proceed(targetMethod);
			System.out.println("two handler end");
			return ret;
		}
	}
}
