package org.springframework.aop;

/**
 * 切面位置
 *
 * Core Spring pointcut abstraction.
 *
 * <p>
 *
 * A pointcut is composed of a {@link ClassFilter} and a {@link MethodMatcher}.
 * Both these basic terms and a Pointcut itself can be combined to build up combinations
 * (e.g. through {@link org.springframework.aop.support.ComposablePointcut}).
 *
 * -- 切入点由{@link ClassFilter}和{@link MethodMatcher}组成。
 * 这些基本术语和Pointcut本身都可以组合起来以建立组合
 * （例如，通过{@link org.springframework.aop.support.ComposablePointcut}）。
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
	 * Pointcut 持有类过滤器，过滤器作用：判断某个类是否符合切点位置
	 *
	 * @return the ClassFilter (never {@code null}) 类过滤器
	 */
	ClassFilter getClassFilter();

	/**
	 * Return the MethodMatcher for this pointcut.
	 *
	 * Pointcut 持有方法匹配器，作用：判断类中某个方法是否符合切点位置
	 *
	 * @return the MethodMatcher (never {@code null}) 方法匹配，在一个类匹配成功之后，继续匹配类中的方法
	 */
	MethodMatcher getMethodMatcher();

	/**
	 * Canonical Pointcut instance that always matches.
	 * 始终匹配的Canonical Pointcut实例。
	 */
	Pointcut TRUE = TruePointcut.INSTANCE;
}