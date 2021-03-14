package com.shengsiyuan.spring.lecture.annotation;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * PersonConfiguration
 *
 * @author zhucj
 * @since 20210325
 */
@Configuration
public class PersonConfiguration {

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