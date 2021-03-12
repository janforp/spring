package com.javaxxl.aop1_自己实现的拦截器链表;

import java.lang.reflect.Method;

/**
 * TargetMethod
 *
 * @author zhucj
 * @since 20210225
 */
public class TargetMethod {

	/**
	 * 目标对象/被代理对象
	 */
	private Object target;

	/**
	 * 被代理接口方法
	 */
	private Method method;

	/**
	 * 被代理接口方法参数
	 */
	private Object[] args;

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public TargetMethod(Object target, Method method, Object[] args) {
		this.target = target;
		this.method = method;
		this.args = args;
	}
}
