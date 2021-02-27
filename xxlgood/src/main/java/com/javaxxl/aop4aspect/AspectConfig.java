package com.javaxxl.aop4aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory;

/**
 * AspectConfig
 *
 * @author zhucj
 * @see ReflectiveAspectJAdvisorFactory#getAdvisors(org.springframework.aop.aspectj.annotation.MetadataAwareAspectInstanceFactory)
 * @since 20210225
 */
@Aspect
public class AspectConfig {

	/**
	 * com.javaxxl 包下，的所有的
	 *
	 * @see ReflectiveAspectJAdvisorFactory#getAdvisors(org.springframework.aop.aspectj.annotation.MetadataAwareAspectInstanceFactory)
	 */
	@Pointcut(value = "execution(* com.javaxxl..*.*test(..))")
	public void test() {
	}

	/**
	 * @see ReflectiveAspectJAdvisorFactory#getAdvisors(org.springframework.aop.aspectj.annotation.MetadataAwareAspectInstanceFactory)
	 */
	@Before(value = "test()")
	public void beforeAdvice() {
		System.out.println("before advice");
	}

	@After(value = "test()")
	public void afterAdvice() {
		System.out.println("after advice");
	}

	@Around(value = "execution(* com.javaxxl..*.*Target(..))")
	public void aroundAdvice(ProceedingJoinPoint joinPoint) {
		System.out.println("around advice begin");
		try {
			joinPoint.proceed();
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
		System.out.println("around advice end");
	}
}
