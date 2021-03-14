package org.springframework.beans.factory.config;

/**
 * Callback for customizing a given bean definition.
 * Designed for use with a lambda expression or method reference.
 * -- 用于自定义给定bean定义的回调。 设计用于lambda表达式或方法引用。
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder#applyCustomizers
 * @since 5.0
 */
@FunctionalInterface
public interface BeanDefinitionCustomizer {

	/**
	 * Customize the given bean definition.
	 */
	void customize(BeanDefinition bd);
}