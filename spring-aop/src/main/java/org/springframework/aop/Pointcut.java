package org.springframework.aop;

/**
 * 切面位置
 *
 * Core Spring pointcut abstraction.
 *
 * <p>A pointcut is composed of a {@link ClassFilter} and a {@link MethodMatcher}.
 * Both these basic terms and a Pointcut itself can be combined to build up combinations
 * (e.g. through {@link org.springframework.aop.support.ComposablePointcut}).
 *
 * @author Rod Johnson
 * @see ClassFilter
 * @see MethodMatcher
 * @see org.springframework.aop.support.Pointcuts
 * @see org.springframework.aop.support.ClassFilters
 * @see org.springframework.aop.support.MethodMatchers
 *
 * 切面位置
 *
 * Pointcut 持有类过滤器，过滤器作用：判断某个类是否符合切点位置
 *
 * Pointcut 持有方法匹配器，作用：判断类中某个方法是否符合切点位置
 */
public interface Pointcut {

	/**
	 * Return the ClassFilter for this pointcut.
	 *
	 * @return the ClassFilter (never {@code null})
	 */
	ClassFilter getClassFilter();

	/**
	 * Return the MethodMatcher for this pointcut.
	 *
	 * @return the MethodMatcher (never {@code null})
	 */
	MethodMatcher getMethodMatcher();

	/**
	 * Canonical Pointcut instance that always matches.
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;
}