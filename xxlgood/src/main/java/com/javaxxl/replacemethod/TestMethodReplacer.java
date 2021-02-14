package com.javaxxl.replacemethod;

import org.springframework.beans.factory.support.MethodReplacer;

import java.lang.reflect.Method;

/**
 * TestMethodReplacer
 *
 * @author zhucj
 * @since 20210225
 */
public class TestMethodReplacer implements MethodReplacer {

	@Override
	public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
		System.out.println("我替换了之前的方法");
		return null;
	}
}