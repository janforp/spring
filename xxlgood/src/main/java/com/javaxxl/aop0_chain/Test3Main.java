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
		//创建被代理对象
		Cat cat = new Cat();

		//组装责任链 start
		//head --> oneHandler --> twoHandler
		AbstractHandler headHandler = new AbstractHandler.HeadHandler();
		headHandler.setNextHandler(new OneHandler()).setNextHandler(new TwoHandler());
		//组装责任链 end

		//创建代理对象
		JdkDynamicProxy jdkDynamicProxy = new JdkDynamicProxy(cat, headHandler);
		//获取代理对象的实例
		Animal proxy = (Animal) jdkDynamicProxy.getProxy();

		//通过代理对象实例调用方法
		proxy.eat();
	}

	private static class OneHandler extends AbstractHandler {

		@Override
		Object invoke(TargetMethod targetMethod) throws InvocationTargetException, IllegalAccessException {
			System.out.println("责任链上的第一个增强 one handler begin");
			Object ret = proceed(targetMethod);
			System.out.println("责任链上的第一个增强 one handler end");
			return ret;
		}
	}

	private static class TwoHandler extends AbstractHandler {

		@Override
		Object invoke(TargetMethod targetMethod) throws InvocationTargetException, IllegalAccessException {
			System.out.println("责任链上的第二个增强 two handler begin");
			Object ret = proceed(targetMethod);
			System.out.println("责任链上的第二个增强 two handler end");
			return ret;
		}
	}
}
