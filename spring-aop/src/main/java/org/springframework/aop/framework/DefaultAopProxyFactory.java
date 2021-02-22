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

	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		if (!IN_NATIVE_IMAGE
				&&
				(
						config.isOptimize()
								||
								config.isProxyTargetClass()
								||
								hasNoUserSuppliedProxyInterfaces(config)
				)) {

			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: "
						+ "Either an interface or a target is required for proxy creation.");
			}
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				//如果被代理class是接口或者本身就是jdk的代理类，则使用jdk的动态代理
				return new JdkDynamicAopProxy(config);
			}
			//否则使用cglib代理
			return new ObjenesisCglibAopProxy(config);
		} else {
			//使用jdk的动态代理
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}
}