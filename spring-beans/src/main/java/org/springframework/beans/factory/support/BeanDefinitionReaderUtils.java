/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods that are useful for bean definition reader implementations.
 * Mainly intended for internal use.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see PropertiesBeanDefinitionReader
 * @see org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader
 * @since 1.1
 */
public abstract class BeanDefinitionReaderUtils {

	/**
	 * Separator for generated bean names. If a class name or parent name is not
	 * unique, "#1", "#2" etc will be appended, until the name becomes unique.
	 */
	public static final String GENERATED_BEAN_NAME_SEPARATOR = BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;

	/**
	 * 如果标签为：
	 * <bean id="componentA" name="a,aa, aaa" class="com.javaxxl.ComponentA">
	 * <description>不可描述</description>
	 * </bean>
	 *
	 * 则参数：
	 * Create a new GenericBeanDefinition for the given parent name and class name, eagerly loading the bean class if a ClassLoader has been specified.
	 * 为给定的父名称和类名称创建一个新的GenericBeanDefinition，如果已指定ClassLoader，则急于加载Bean类。(beanClass)
	 *
	 * @param parentName the name of the parent bean, if any 为null
	 * @param className the name of the bean class, if any 为 com.javaxxl.ComponentA
	 * @param classLoader the ClassLoader to use for loading bean classes 为null
	 * (can be {@code null} to just register bean classes by name)
	 * @return the bean definition
	 * @throws ClassNotFoundException if the bean class could not be loaded
	 */
	public static AbstractBeanDefinition createBeanDefinition(
			@Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setParentName(parentName);
		if (className != null) {
			if (classLoader != null) {
				//如果有类加载器，则通过方式加载class到jvm
				//通过反射的方式设置  beanClass 字段
				bd.setBeanClass(

						/**
						 * 获取 className 对应的 Class 对象
						 * @see AbstractBeanDefinition#hasBeanClass()
						 * @see AbstractBeanDefinition#beanClass 为 Class 对象
						 */
						ClassUtils.forName(className, classLoader)
				);
			} else {

				/**
				 * 如果没有类加载器，则保留 bean 的class属性值
				 * @see AbstractBeanDefinition#beanClass 为字符串
				 */
				bd.setBeanClassName(className);
			}
		}
		return bd;
	}

	/**
	 * Generate a bean name for the given top-level bean definition,
	 * unique within the given bean factory.
	 *
	 * @param beanDefinition the bean definition to generate a bean name for
	 * @param registry the bean factory that the definition is going to be
	 * registered with (to check for existing bean names)
	 * @return the generated bean name
	 * @throws BeanDefinitionStoreException if no unique name can be generated
	 * for the given bean definition
	 * @see #generateBeanName(BeanDefinition, BeanDefinitionRegistry, boolean)
	 */
	public static String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		return generateBeanName(beanDefinition, registry, false);
	}

	/**
	 * Generate a bean name for the given bean definition, unique within the
	 * given bean factory.
	 *
	 * @param definition the bean definition to generate a bean name for
	 * @param registry the bean factory that the definition is going to be
	 * registered with (to check for existing bean names)
	 * @param isInnerBean whether the given bean definition will be registered
	 * as inner bean or as top-level bean (allowing for special name generation
	 * for inner beans versus top-level beans)
	 * @return the generated bean name
	 * @throws BeanDefinitionStoreException if no unique name can be generated
	 * for the given bean definition
	 */
	public static String generateBeanName(
			BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean)
			throws BeanDefinitionStoreException {

		String generatedBeanName = definition.getBeanClassName();
		if (generatedBeanName == null) {
			if (definition.getParentName() != null) {
				generatedBeanName = definition.getParentName() + "$child";
			} else if (definition.getFactoryBeanName() != null) {
				generatedBeanName = definition.getFactoryBeanName() + "$created";
			}
		}
		if (!StringUtils.hasText(generatedBeanName)) {
			throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither 'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
		}

		if (isInnerBean) {
			// Inner bean: generate identity hashcode suffix.
			return generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
		}

		// Top-level bean: use plain class name with unique suffix if necessary.
		//顶级bean：必要时使用带有唯一后缀的普通类名。
		return uniqueBeanName(generatedBeanName, registry);
	}

	/**
	 * Turn the given bean name into a unique bean name for the given bean factory,
	 * appending a unique counter as suffix if necessary.
	 *
	 * @param beanName the original bean name
	 * @param registry the bean factory that the definition is going to be
	 * registered with (to check for existing bean names)
	 * @return the unique bean name to use
	 * @since 5.1
	 */
	public static String uniqueBeanName(String beanName, BeanDefinitionRegistry registry) {
		String id = beanName;
		int counter = -1;

		// Increase counter until the id is unique.一个 # 号
		String prefix = beanName + GENERATED_BEAN_NAME_SEPARATOR;
		while (counter == -1 || registry.containsBeanDefinition(id)) {
			//保证唯一性
			counter++;
			id = prefix + counter;
		}
		return id;
	}

	/**
	 * Register the given bean definition with the given bean factory.
	 *
	 * --在给定的bean工厂中注册给定的bean定义。
	 *
	 * @param definitionHolder the bean definition including name and aliases 解析xml标签得到的 bdHolder
	 * @param registry the bean factory to register with 注册中心
	 * @throws BeanDefinitionStoreException if registration failed
	 */
	public static void registerBeanDefinition(
			BeanDefinitionHolder definitionHolder,//bd，完成的<bean>标签
			BeanDefinitionRegistry registry)//工厂

			throws BeanDefinitionStoreException {

		// Register bean definition under primary name.
		String beanName = definitionHolder.getBeanName();

		/**
		 * 注册bd到registry
		 * @see DefaultListableBeanFactory#registerBeanDefinition(java.lang.String, org.springframework.beans.factory.config.BeanDefinition)
		 * @see DefaultListableBeanFactory#beanDefinitionMap 其实就保存到这里了
		 */
		registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

		// Register aliases for bean name, if any.：注册bean名称的别名（如果有）。
		String[] aliases = definitionHolder.getAliases();
		if (aliases != null) {
			for (String alias : aliases) {
				/**
				 * @see SimpleAliasRegistry#registerAlias(java.lang.String, java.lang.String)
				 */
				registry.registerAlias(beanName, alias);
			}
		}
	}

	/**
	 * Register the given bean definition with a generated name,
	 * unique within the given bean factory.
	 *
	 * @param definition the bean definition to generate a bean name for
	 * @param registry the bean factory to register with
	 * @return the generated bean name
	 * @throws BeanDefinitionStoreException if no unique name can be generated
	 * for the given bean definition or the definition cannot be registered
	 */
	public static String registerWithGeneratedName(
			AbstractBeanDefinition definition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		String generatedName = generateBeanName(definition, registry, false);
		registry.registerBeanDefinition(generatedName, definition);
		return generatedName;
	}

}
