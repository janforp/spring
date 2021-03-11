package com.javaxxl.aop0_chain;

import java.lang.reflect.InvocationTargetException;

/**
 * AbstractHandler
 *
 * @author zhucj
 * @since 20210225
 */
public abstract class AbstractHandler {

	private AbstractHandler nextHandler;

	abstract Object invoke(TargetMethod targetMethod) throws InvocationTargetException, IllegalAccessException;

	public boolean hasNext() {
		return nextHandler != null;
	}

	public Object proceed(TargetMethod targetMethod) throws InvocationTargetException, IllegalAccessException {
		if (!hasNext()) {
			return targetMethod.getMethod().invoke(targetMethod.getTarget(), targetMethod.getArgs());
		}
		return nextHandler.invoke(targetMethod);
	}

	public void setNextHandler(AbstractHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

	public static class HeadHandler extends AbstractHandler {

		@Override
		Object invoke(TargetMethod targetMethod) {
			return null;
		}
	}
}
