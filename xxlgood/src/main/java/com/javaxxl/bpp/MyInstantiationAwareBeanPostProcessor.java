package com.javaxxl.bpp;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

/**
 * MyInstantiationAwareBeanPostProcessor
 *
 * @author zhucj
 * @since 20210225
 */
public class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		System.out.println("----> postProcessBeforeInstantiation");
		System.out.println(beanClass);
		System.out.println(beanName);
		if (beanClass == Student.class) {
			Enhancer e = new Enhancer();
			e.setSuperclass(beanClass);
			e.setCallback((MethodInterceptor) (o, method, objects, methodProxy) -> {
				System.out.println("目标方法执行前：" + method + "\n");
				Object invokeSuper = methodProxy.invokeSuper(o, objects);
				System.out.println("目标方法执行后：" + method + "\n");
				return invokeSuper;
			});
			return e.create();
		}
		return null;
	}
}