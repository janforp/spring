package org.springframework.aop.support;

import org.aopalliance.aop.Advice;

/**
 * Abstract generic {@link org.springframework.aop.PointcutAdvisor}
 * that allows for any {@link Advice} to be configured.
 *
 * @author Juergen Hoeller
 * @see #setAdvice
 * @see DefaultPointcutAdvisor
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractGenericPointcutAdvisor extends AbstractPointcutAdvisor {

	/**
	 * 默认增强为啥都不做
	 *
	 * 啥都不做其实也是一种增强策略
	 */
	private Advice advice = EMPTY_ADVICE;

	/**
	 * Specify the advice that this advisor should apply.
	 */
	public void setAdvice(Advice advice) {
		this.advice = advice;
	}

	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": advice [" + getAdvice() + "]";
	}
}