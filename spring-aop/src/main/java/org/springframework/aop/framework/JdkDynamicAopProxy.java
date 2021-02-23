package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JDK-based {@link AopProxy} implementation for the Spring AOP framework,
 * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 *
 * <p>Creates a dynamic proxy, implementing the interfaces exposed by
 * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
 * defined in classes, rather than interfaces.
 *
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by an {@link AdvisedSupport} class. This class is internal
 * to Spring's AOP framework and need not be used directly by client code.
 *
 * <p>Proxies created using this class will be thread-safe if the
 * underlying (target) class is thread-safe.
 *
 * <p>Proxies are serializable so long as all Advisors (including Advices
 * and Pointcuts) and the TargetSource are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author Sergey Tsypanov
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/**
	 * use serialVersionUID from Spring 1.2 for interoperability.
	 */
	private static final long serialVersionUID = 5531744639992436476L;


	/*
	 * NOTE: We could avoid the code duplication between this class and the CGLIB
	 * proxies by refactoring "invoke" into a template method. However, this approach
	 * adds at least 10% performance overhead versus a copy-paste solution, so we sacrifice
	 * elegance for performance. (We have a good test suite to ensure that the different
	 * proxies behave the same :-)
	 * This way, we can also more easily take advantage of minor optimizations in each class.
	 */

	/**
	 * We use a static Log to avoid serialization issues.
	 */
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	/**
	 * Config used to configure this proxy.
	 *
	 * @see ProxyFactory 一般为该类型
	 */
	private final AdvisedSupport advised;

	/**
	 * 代理类型实现的接口，数组包括被代理接口以及Spring追加接口
	 *
	 * @see AopProxyUtils#completeProxiedInterfaces(org.springframework.aop.framework.AdvisedSupport, boolean)
	 */
	private final Class<?>[] proxiedInterfaces;

	/**
	 * Is the {@link #equals} method defined on the proxied interfaces?
	 *
	 * @see JdkDynamicAopProxy#findDefinedEqualsAndHashCodeMethods(java.lang.Class[]) 查询当前所有需要代理的接口，看看是否有 equals 和 hashcode 方法，如果有，则打一个标记
	 */
	private boolean equalsDefined;

	/**
	 * Is the {@link #hashCode} method defined on the proxied interfaces?
	 *
	 * @see JdkDynamicAopProxy#findDefinedEqualsAndHashCodeMethods(java.lang.Class[]) 查询当前所有需要代理的接口，看看是否有 equals 和 hashcode 方法，如果有，则打一个标记
	 */
	private boolean hashCodeDefined;

	/**
	 * Construct a new JdkDynamicAopProxy for the given AOP configuration.
	 *
	 * @param config the AOP configuration as AdvisedSupport object
	 * @throws AopConfigException if the config is invalid. We try to throw an informative
	 * exception in this case, rather than let a mysterious failure happen later.
	 * @see DefaultAopProxyFactory#createAopProxy(org.springframework.aop.framework.AdvisedSupport) 此处会调用该方法
	 * @see org.springframework.aop.framework.ProxyCreatorSupport 参数 config 一般就是该类型
	 */
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		if (config.getAdvisorCount() == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		this.advised = config;

		/**
		 * 获取需要代理的接口数组
		 * 数组包括被代理接口以及Spring追加接口
		 */
		this.proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
		/**
		 * 查询当前所有需要代理的接口，看看是否有 equals 和 hashcode 方法，如果有，则打一个标记
		 */
		findDefinedEqualsAndHashCodeMethods(this.proxiedInterfaces);
	}

	@Override
	public Object getProxy() {
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
		}

		/**
		 * 很熟悉了吧
		 * 该方法最终会返回一个代理类对象
		 * 代理类大概的样子：
		 *
		 * class $proxy12 extends Proxy implements A,B,C {
		 *
		 *     //代理方法
		 *     void xx() {
		 *     		//调用被代理对象方法
		 *         super.h.invoke(x,xx);
		 *     }
		 * }
		 *
		 * @see JdkDynamicAopProxy#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
		 */
		return Proxy.newProxyInstance(
				classLoader, //类加载器
				this.proxiedInterfaces, //生成代理类需要实现的接口集合

				/**
				 * JdkDynamicAopProxy 其实就是 InvocationHandler
				 * 该类实现了 {@link InvocationHandler} 接口
				 * 该方法最终会返回一个代理类对象
				 */
				this
		);
	}

	/**
	 * 查询当前所有需要代理的接口，看看是否有 equals 和 hashcode 方法，如果有，则打一个标记
	 *
	 * Finds any {@link #equals} or {@link #hashCode} method that may be defined on the supplied set of interfaces.
	 * -- 查找可能在提供的一组接口上定义的任何{@link #equals}或{@link #hashCode}方法。
	 *
	 * @param proxiedInterfaces the interfaces to introspect：数组包括被代理接口以及Spring追加接口
	 */
	private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
		for (Class<?> proxiedInterface : proxiedInterfaces) {
			Method[] methods = proxiedInterface.getDeclaredMethods();
			for (Method method : methods) {
				if (AopUtils.isEqualsMethod(method)) {
					this.equalsDefined = true;
				}
				if (AopUtils.isHashCodeMethod(method)) {
					this.hashCodeDefined = true;
				}
				if (this.equalsDefined && this.hashCodeDefined) {
					return;
				}
			}
		}
	}

	/**
	 * Implementation of {@code InvocationHandler.invoke}.
	 * <p>Callers will see exactly the exception thrown by the target,
	 * unless a hook method throws an exception.
	 *
	 * @param proxy 代理对象
	 * @param method 被代理方法
	 * @param args 被代理方法参数
	 * @return 被代理方法返回
	 * @throws Throwable 如果发生异常
	 */
	@Override
	@Nullable
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object oldProxy = null;
		boolean setProxyContext = false;

		//获取到创建 ProxyFactory 的时候提供的 target
		TargetSource targetSource = this.advised.targetSource;

		//真正的 target 引用
		Object target = null;

		try {
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// 如果代理对象没有实现 equals 方法，并且当前调用方法是 equal 方法，则所以当前 JdkDynamicAopProxy 对象提供的 equal 方法
				// The target does not implement the equals(Object) method itself.：目标本身不实现equals（Object）方法。
				return equals(args[0]);
			} else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// 如果代理对象没有实现 hashcode 方法，并且当前调用方法是 hashcode 方法，则所以当前 JdkDynamicAopProxy 对象提供的 hashcode 方法
				// The target does not implement the hashCode() method itself.：目标本身不实现hashCode（）方法。
				return hashCode();
			} else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.：仅声明了getDecoratedClass（）->分派到代理配置。
				return AopProxyUtils.ultimateTargetClass(this.advised);
			} else if (!this.advised.opaque && method.getDeclaringClass().isInterface() && method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// Service invocations on ProxyConfig with the proxy config...：使用代理配置在ProxyConfig上进行服务调用...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			//保存返回值
			Object retVal;

			/**
			 * 表示是否需要把当前代理对象暴露到 Aop 上下文中
			 * 暴露之后应用程序就能拿到
			 * @see AopContext
			 */
			if (this.advised.exposeProxy) {
				// Make invocation available if necessary.-- 如有必要，使调用可用。
				oldProxy = AopContext.setCurrentProxy(proxy);
				setProxyContext = true;
			}

			// Get as late as possible to minimize the time we "own" the target, in case it comes from a pool.
			// TODO 如果目标来自一个池，则应尽可能晚些以最小化我们“拥有”目标的时间。
			//拿到目标对象/被代理对象
			target = targetSource.getTarget();
			//目标对象的类
			Class<?> targetClass = (target != null ? target.getClass() : null);

			/**
			 * 其实，这里最关键的地方，查找适合该方法的增强(方法拦截器)
			 * 查询匹配该方法的增强
			 * Get the interception chain for this method.:获取此方法的拦截链。
			 * @see DefaultAdvisorAdapterRegistry#getInterceptors(org.springframework.aop.Advisor)
			 */
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// Check whether we have any advice. If we don't, we can fallback on direct
			// reflective invocation of the target, and avoid creating a MethodInvocation.
			if (chain.isEmpty()) {
				/**
				 * 如果匹配当前方法的拦截器为空，说明当前方法不需要被增强，直接调用目标对象的方法即可
				 */
				// We can skip creating a MethodInvocation: just invoke the target directly
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);

				//直接调用目标对象的方法
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			} else {
				/**
				 * 有匹配当前方法的拦截器
				 * @see com.javaxxl.aop3.Main
				 */
				// We need to create a method invocation...:们需要创建一个方法调用...
				MethodInvocation invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// Proceed to the joinpoint through the interceptor chain.:通过拦截器链进入连接点。
				/**
				 * TODO 核心逻辑
				 * @see ReflectiveMethodInvocation#proceed()
				 */
				retVal = invocation.proceed();
			}

			//核心逻辑已经完成

			// Massage return value if necessary.

			//获取方法返回值类型
			Class<?> returnType = method.getReturnType();
			if (retVal != null
					&& retVal == target //方法返回了目标对象，则返回代理对象
					&&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// Special case: it returned "this" and the return type of the method
				// is type-compatible. Note that we can't help if the target sets
				// a reference to itself in another returned object.

				/**
				 * 方法返回了目标对象，可能是链式编程
				 * 则返回代理对象
				 */
				retVal = proxy;
			} else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				//元素类型但是返回了null，则npe
				throw new AopInvocationException("Null return value from advice does not match primitive return type for: " + method);
			}
			return retVal;
		} finally {
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource.
				targetSource.releaseTarget(target);
			}
			if (setProxyContext) {
				/**
				 * Restore old proxy.：把老的代理对象(上次)再次存进去
				 * TODO ??
				 * 因为当前代理对象的方法已经完事了，需要回到上一层逻辑了
				 * 这里是一个恢复现场的逻辑
				 */
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}

	/**
	 * Equality means interfaces, advisors and TargetSource are equal.
	 * <p>The compared object may be a JdkDynamicAopProxy instance itself
	 * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy) {
			otherProxy = (JdkDynamicAopProxy) other;
		} else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy)) {
				return false;
			}
			otherProxy = (JdkDynamicAopProxy) ih;
		} else {
			// Not a valid comparison...
			return false;
		}

		// If we get here, otherProxy is the other AopProxy.
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * Proxy uses the hash code of the TargetSource.
	 */
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}
}