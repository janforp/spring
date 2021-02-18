package com.javaxxl.beanFactoryBeanPostProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringValueResolver;

import java.util.HashSet;
import java.util.Set;

/**
 * 容器后处理器
 *
 * 处理敏感 beanName
 *
 * @author zhucj
 * @since 20210225
 */
public class ReplaceObscenitiesBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private final Set<String> obscenities;

	public ReplaceObscenitiesBeanFactoryPostProcessor() {
		this.obscenities = new HashSet<>();
	}

	public void setObscenities(Set<String> obscenities) {
		this.obscenities.clear();
		this.obscenities.addAll(obscenities);
	}

	public boolean isObscene(Object value) {
		String upperCase = value.toString().toUpperCase();
		return this.obscenities.contains(upperCase);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] beanNames = beanFactory.getBeanDefinitionNames();
		for (String beanName : beanNames) {
			BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
			StringValueResolver valueResolver = strVal -> {
				if (isObscene(strVal)) {
					return "******";
				}
				return strVal;
			};
			BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);
			visitor.visitBeanDefinition(bd);
		}
	}
}