package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * obtain：获得，保存
 *
 * A {@code TargetSource} is used to obtain the current "target" of
 * an AOP invocation, which will be invoked via reflection if no around
 * advice chooses to end the interceptor chain itself.
 *
 * -- {@code TargetSource}用于获取AOP调用的当前“目标（对代理对象）”，如果当前拦截器链表中已经没有增强可以调用了，则通过发射的方式调用该目标对象
 *
 * <p>
 * If a {@code TargetSource} is "static", it will always return
 * the same target, allowing optimizations in the AOP framework.
 * -- 如果{@code TargetSource}是“ static”，它将始终返回相同的目标，从而允许在AOP框架中进行优化。
 *
 * Dynamic target sources can support pooling, hot swapping, etc.
 * -- 动态目标源可以支持池化，热插拔等。
 *
 * <p>
 * Application developers don't usually need to work with {@code TargetSources} directly: this is an AOP framework interface.
 * -- 应用程序开发人员通常不需要直接使用{@code TargetSources}：这是一个AOP框架接口
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface TargetSource extends TargetClassAware {

	/**
	 * Return the type of targets returned by this {@link TargetSource}.
	 * <p>Can return {@code null}, although certain usages of a {@code TargetSource}
	 * might just work with a predetermined target class.
	 *
	 * @return the type of targets returned by this {@link TargetSource}
	 */
	@Override
	@Nullable
	Class<?> getTargetClass();

	/**
	 * Will all calls to {@link #getTarget()} return the same object?
	 * <p>In that case, there will be no need to invoke {@link #releaseTarget(Object)},
	 * and the AOP framework can cache the return value of {@link #getTarget()}.
	 *
	 * @return {@code true} if the target is immutable
	 * @see #getTarget
	 */
	boolean isStatic();

	/**
	 * Return a target instance. Invoked immediately before the
	 * AOP framework calls the "target" of an AOP method invocation.
	 *
	 * @return the target object which contains the joinpoint,
	 * or {@code null} if there is no actual target instance
	 * @throws Exception if the target object can't be resolved
	 */
	@Nullable
	Object getTarget() throws Exception;

	/**
	 * Release the given target object obtained from the
	 * {@link #getTarget()} method, if any.
	 *
	 * @param target object obtained from a call to {@link #getTarget()}
	 * @throws Exception if the object can't be released
	 */
	void releaseTarget(Object target) throws Exception;
}