package org.aopalliance.intercept;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

/**
 * Description of an invocation to a method, given to an interceptor upon method-call.
 *
 * <p>A method invocation is a joinpoint and can be intercepted by a
 * method interceptor.
 *
 * @author Rod Johnson
 * @see org.springframework.aop.framework.JdkDynamicAopProxy#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
 * @see MethodInterceptor
 */
public interface MethodInvocation extends Invocation {

	/**
	 * Get the method being called.
	 * <p>This method is a friendly implementation of the
	 * {@link Joinpoint#getStaticPart()} method (same result).
	 *
	 * @return the method being called
	 */
	@Nonnull
	Method getMethod();
}