package org.springframework.aop.framework;

import org.springframework.aop.SpringProxy;

import java.io.Serializable;
import java.lang.reflect.Proxy;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 * @since 12.03.2004
 */
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	/**
	 * Whether this environment lives within a native image.
	 * Exposed as a private static field rather than in a {@code NativeImageDetector.inNativeImage()} static method due to https://github.com/oracle/graal/issues/2594.
	 *
	 * @see <a href="https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java">ImageInfo.java</a>
	 */
	private static final boolean IN_NATIVE_IMAGE = (System.getProperty("org.graalvm.nativeimage.imagecode") != null);

	/**
	 * @param config the AOP configuration in the form of an
	 * AdvisedSupport object
	 * @return 一个生成代理对象的实例
	 * @throws AopConfigException 异常
	 * @see org.springframework.aop.framework.ProxyCreatorSupport
	 * @see ProxyFactory
	 */
	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		if (!IN_NATIVE_IMAGE
				&&
				(
						config.isOptimize()//暂且不管
								||
								config.isProxyTargetClass()//如果该条件为 true,表示强制使用 cglib 动态代理
								||
								hasNoUserSuppliedProxyInterfaces(config)//如果该条件为 true,说明被代理对象没有实现然后接口，无法使用jdk动态代理，只能使用cglib
				)) {

			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " + "Either an interface or a target is required for proxy creation.");
			}
			if (targetClass.isInterface() //一个接口

					/**
					 * 或者该类型已经是一个被代理过的类型
					 * 该类型已经是一个jdk动态代理对象来
					 */
					|| Proxy.isProxyClass(targetClass)) {

				//如果被代理class是接口或者该类型已经是一个被代理过的类型，则使用jdk的动态代理
				return new JdkDynamicAopProxy(config);
			}
			//否则使用cglib代理
			return new ObjenesisCglibAopProxy(config);
		}

		//使用jdk的动态代理
		else {
			/**
			 * targetClass实现了接口的情况下，一般会走该分支！
			 * 我们一般但是面向接口编程，所以我们只研究jdk动态代理
			 */
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * 如果该条件为 true,说明被代理对象没有实现然后接口，无法使用jdk动态代理，只能使用cglib
	 *
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (
				//没有实现任何接口
				ifcs.length == 0
						||
						//只实现一个接口，并且还是 SpringProxy 接口（内部接口）
						(ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0]))
		);
	}
}