package org.springframework.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.AfterAdvice;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Interceptor to wrap an {@link org.springframework.aop.AfterReturningAdvice}.
 * Used internally by the AOP framework; application developers should not need
 * to use this class directly.
 *
 * @author Rod Johnson
 * @see MethodBeforeAdviceInterceptor
 * @see ThrowsAdviceInterceptor
 */
@SuppressWarnings("serial")
public class AfterReturningAdviceInterceptor
		implements MethodInterceptor, AfterAdvice, Serializable {

	private final AfterReturningAdvice advice;

	/**
	 * Create a new AfterReturningAdviceInterceptor for the given advice.
	 *
	 * @param advice the AfterReturningAdvice to wrap
	 */
	public AfterReturningAdviceInterceptor(AfterReturningAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		Object retVal = mi.proceed();
		this.advice.afterReturning(
				retVal,
				mi.getMethod(),
				mi.getArguments(),
				mi.getThis());

		return retVal;
	}
}