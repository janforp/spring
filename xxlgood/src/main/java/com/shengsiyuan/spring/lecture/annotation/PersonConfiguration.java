package com.shengsiyuan.spring.lecture.annotation;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Scope;

/**
 * PersonConfiguration
 *
 * @author zhucj
 * @since 20210325
 */
@Configuration
public class PersonConfiguration {

	/**
	 * 通过{@link ConfigurationClassPostProcessor#processConfigBeanDefinitions(org.springframework.beans.factory.support.BeanDefinitionRegistry)} 把该方法解析成 bd
	 *
	 * @see org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader 最终通过这个reader把该方法解析成一个bd
	 */
	@Bean(name = "person")
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public Person getPerson() {
		System.out.println("getPerson 方法被调用了");

		Person person = new Person();
		person.setId(1);
		person.setName("zhangSan");
		return person;
	}
}