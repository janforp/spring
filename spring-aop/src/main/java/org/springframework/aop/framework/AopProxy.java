package org.springframework.aop.framework;

import org.springframework.lang.Nullable;

/**
 * Delegate interface for a configured AOP proxy, allowing for the creation of actual proxy objects.
 * -- 用于配置的AOP代理的委托接口，允许创建实际的代理对象。
 *
 * <p>Out-of-the-box implementations are available for JDK dynamic proxies
 * and for CGLIB proxies, as applied by {@link DefaultAopProxyFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DefaultAopProxyFactory
 * @see CglibAopProxy 生成代理对象方法1
 * @see JdkDynamicAopProxy 生成代理对象方法2，代理接口
 * @see ObjenesisCglibAopProxy  生成代理对象方法3，代理非接口的默认方法
 */
public interface AopProxy {

	/**
	 * Create a new proxy object.
	 * <p>Uses the AopProxy's default class loader (if necessary for proxy creation):
	 * usually, the thread context class loader.
	 *
	 * @return the new proxy object (never {@code null})
	 * @see Thread#getContextClassLoader()
	 * @see DefaultAopProxyFactory#createAopProxy(org.springframework.aop.framework.AdvisedSupport)
	 */
	Object getProxy();

	/**
	 * Create a new proxy object.
	 * <p>Uses the given class loader (if necessary for proxy creation).
	 * {@code null} will simply be passed down and thus lead to the low-level
	 * proxy facility's default, which is usually different from the default chosen
	 * by the AopProxy implementation's {@link #getProxy()} method.
	 *
	 * @param classLoader the class loader to create the proxy with
	 * (or {@code null} for the low-level proxy facility's default)
	 * @return the new proxy object (never {@code null})
	 */
	Object getProxy(@Nullable ClassLoader classLoader);
}