package com.shengsiyuan.spring.lecture.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PersonConfiguration
 *
 * @author zhucj
 * @since 20210325
 */
@Configuration
public class PersonConfiguration {

	@Bean(name = "person")
	public Person getPerson() {
		Person person = new Person();
		person.setId(1);
		person.setName("zhangSan");
		return person;
	}
}