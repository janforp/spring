package org.springframework.aop.config;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.w3c.dom.Element;

/**
 * Utility class for handling registration of auto-proxy creators used internally
 * by the '{@code aop}' namespace tags.
 *
 * <p>Only a single auto-proxy creator should be registered and multiple configuration
 * elements may wish to register different concrete implementations. As such this class
 * delegates to {@link AopConfigUtils} which provides a simple escalation protocol.
 * Callers may request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @see AopConfigUtils
 * @since 2.0
 */
public abstract class AopNamespaceUtils {

	/**
	 * The {@code proxy-target-class} attribute as found on AOP-related XML tags.
	 */
	public static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

	/**
	 * The {@code expose-proxy} attribute as found on AOP-related XML tags.
	 */
	private static final String EXPOSE_PROXY_ATTRIBUTE = "expose-proxy";

	public static void registerAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	public static void registerAspectJAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	/**
	 * 方法名称：如有必要，注册Aspect J注释自动代理创建器
	 *
	 * @param sourceElement 标签:<aop:aspectj-autoproxy/> 处理自定义标签
	 * @param parserContext 封装解析过程当前状态的对象，内部持有 readContext,readContext持有registry也就是咋们的BeanFactory
	 */
	public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			ParserContext parserContext,//封装解析过程当前状态的对象，内部持有 readContext,readContext持有registry也就是咋们的BeanFactory
			Element sourceElement) { // 标签

		BeanDefinition beanDefinition

				/**
				 * 方法名称:如有必要，注册Aspect J注释自动代理创建器
				 *
				 * 该方法你会把 aspectj-autoproxy 解析成一个 beanDefinition 并且注册到容器内
				 * @see AopConfigUtils#registerOrEscalateApcAsRequired(java.lang.Class, org.springframework.beans.factory.support.BeanDefinitionRegistry, java.lang.Object)
				 *
				 * @see AnnotationAwareAspectJAutoProxyCreator bd 的 class 类型为 AnnotationAwareAspectJAutoProxyCreator
				 */
				= AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), // BeanDefinitionRegistry
				parserContext.extractSource(sourceElement));

		//执行到这里，spring 容器中已经有了 一个 AOP 相关的 bd 了

		/**
		 * 进一步处理 <aop:aspectj-autoproxy/>
		 * <aop:aspectj-autoproxy  proxy-target-class = "true" />
		 * <aop:aspectj-autoproxy expose-proxy="true"/>
		 *
		 * 处理2个属性
		 */
		useClassProxyingIfNecessary(parserContext.getRegistry()/**spring 容器*/, sourceElement /** aop 标签*/);

		//注册组件:暂时不分洗
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	private static void useClassProxyingIfNecessary(
			BeanDefinitionRegistry registry, // spring容器
			@Nullable Element sourceElement) { // <aop:aspectj-autoproxy/> 标签

		if (sourceElement != null) {

			//是否代理目标类
			boolean proxyTargetClass = Boolean.parseBoolean(
					/**
					 * <aop:aspectj-autoproxy  proxy-target-class = "true" />
					 *
					 * proxy-target-class 属性的值,默认为 false，如果为 true,则
					 * 不管被代理类是否实现接口,都使用 cglib 代理
					 */
					sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));

			if (proxyTargetClass) {
				//如果 <aop:aspectj-autoproxy  proxy-target-class = "true" />
				//向 aop 的 bd 中添加一个 proxyTargetClass 属性
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}

			/**
			 *  expose-proxy属性的值
			 *  <aop:aspectj-autoproxy expose-proxy="true"/>
			 *
			 *  是否将当前代理对象暴露到 上下文，方便代理对象内部的被代理对象 拿到 代理对象，用来解决 被代理对象方法 存在
			 *
			 *  嵌套的情况，嵌套方法无法得到增强的情况
			 *
			 * @see org.springframework.aop.framework.AopContext
			 */
			boolean exposeProxy = Boolean.parseBoolean(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
			if (exposeProxy) {
				//如果 <aop:aspectj-autoproxy  expose-proxy = "true" />
				//向 aop 的 bd 中添加一个 exposeProxy 属性
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}

	private static void registerComponentIfNecessary(
			@Nullable BeanDefinition beanDefinition,
			ParserContext parserContext) {
		if (beanDefinition != null) {
			parserContext.registerComponent(
					new BeanComponentDefinition(beanDefinition, AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)
			);
		}
	}
}