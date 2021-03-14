package org.springframework.context.annotation;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Delegate factory class used to just introduce an AOP framework dependency
 * when actually creating a scoped proxy.
 *
 * @author Juergen Hoeller
 * @see org.springframework.aop.scope.ScopedProxyUtils#createScopedProxy
 * @since 3.0
 */
final class ScopedProxyCreator {

	private ScopedProxyCreator() {
	}

	public static BeanDefinitionHolder createScopedProxy(
			BeanDefinitionHolder definitionHolder,//通过注解配置的bean解析之后的 bd
			BeanDefinitionRegistry registry, //ioc容器
			boolean proxyTargetClass      //是否代理目标类，而不是接口，如果是，则使用cglib，否则使用jdk
	) {

		return ScopedProxyUtils.createScopedProxy(definitionHolder, registry, proxyTargetClass);
	}

	public static String getTargetBeanName(String originalBeanName) {
		return ScopedProxyUtils.getTargetBeanName(originalBeanName);
	}
}