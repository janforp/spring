package org.springframework.aop.framework;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * Class containing static methods used to obtain information about the current AOP invocation.
 *
 * <p>The {@code currentProxy()} method is usable if the AOP framework is configured to
 * expose the current proxy (not the default). It returns the AOP proxy in use. Target objects
 * or advice can use this to make advised calls, in the same way as {@code getEJBObject()}
 * can be used in EJBs. They can also use it to find advice configuration.
 *
 * <p>Spring's AOP framework does not expose proxies by default, as there is a performance cost
 * in doing so.
 *
 * <p>The functionality in this class might be used by a target object that needed access
 * to resources on the invocation. However, this approach should not be used when there is
 * a reasonable alternative, as it makes application code dependent on usage under AOP and
 * the Spring AOP framework in particular.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13.03.2003
 */
public final class AopContext {

	/**
	 * 解决的问题：
	 *
	 * 假设被代理类为
	 * class A {
	 *
	 * --void methodA() {
	 * ----methodB();
	 * --}
	 *
	 * --void methodB() {
	 *
	 * --}
	 * }
	 *
	 * 方法A中调用了方法B，如果在A的代理对象 proxyA上调用方法methodA，
	 * 则会调用 super.h.invoke(methodA, args)
	 *
	 * 最终会进入目标对象的 methodA 方法，在方法 methodA 中是提供 this.methodB()调用B的，那么此时就
	 * 有问题了，此时只增强了方法methodA并没有增强方法methodB，因为此时是在被代理对象内部调用methodA而不是在
	 * 代理对象 proxyA 中调用，所以无法增强
	 *
	 * 解决方案：
	 *
	 * 如果把当前代理对象保存到 currentProxy, 则在 被代理对象 中调用 methodA 的时候，强行修改 methodA 的实现，使 methodB
	 * 改成 proxyA.methodB()，此时方法methodB也能得到增强
	 *
	 * ThreadLocal holder for AOP proxy associated with this thread.
	 * Will contain {@code null} unless the "exposeProxy" property on
	 * the controlling proxy configuration has been set to "true".
	 *
	 * @see ProxyConfig#setExposeProxy
	 */
	private static final ThreadLocal<Object> currentProxy = new NamedThreadLocal<>("Current AOP proxy");

	private AopContext() {
	}

	/**
	 * Try to return the current AOP proxy. This method is usable only if the
	 * calling method has been invoked via AOP, and the AOP framework has been set
	 * to expose proxies. Otherwise, this method will throw an IllegalStateException.
	 *
	 * @return the current AOP proxy (never returns {@code null})
	 * @throws IllegalStateException if the proxy cannot be found, because the
	 * method was invoked outside an AOP invocation context, or because the
	 * AOP framework has not been configured to expose the proxy
	 */
	public static Object currentProxy() throws IllegalStateException {
		Object proxy = currentProxy.get();
		if (proxy == null) {
			throw new IllegalStateException(
					"Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it available, and " +
							"ensure that AopContext.currentProxy() is invoked in the same thread as the AOP invocation context.");
		}
		return proxy;
	}

	/**
	 * Make the given proxy available via the {@code currentProxy()} method.
	 * <p>Note that the caller should be careful to keep the old value as appropriate.
	 *
	 * @param proxy the proxy to expose (or {@code null} to reset it) 一个代理对象
	 * @return the old proxy, which may be {@code null} if none was bound
	 * @see #currentProxy()
	 */
	@Nullable
	static Object setCurrentProxy(@Nullable Object proxy) {
		Object old = currentProxy.get();
		if (proxy != null) {
			currentProxy.set(proxy);
		} else {
			currentProxy.remove();
		}
		return old;
	}
}