package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.MethodMatcher;

/**
 * Internal framework class, combining a MethodInterceptor instance
 * with a MethodMatcher for use as an element in the advisor chain.
 *
 * @author Rod Johnson
 */
class InterceptorAndDynamicMethodMatcher {

	final MethodInterceptor interceptor;

	final MethodMatcher methodMatcher;

	public InterceptorAndDynamicMethodMatcher(MethodInterceptor interceptor, MethodMatcher methodMatcher) {
		this.interceptor = interceptor;
		this.methodMatcher = methodMatcher;
	}
}