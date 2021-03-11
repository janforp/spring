package com.shengsiyuan.spring.lecture.aop;

import com.shengsiyuan.spring.lecture.aop.service.MyService;
import org.springframework.aop.SpringProxy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.Arrays;

/**
 * Test
 *
 * @author zhucj
 * @since 20210325
 */
public class SpringClientAop {

	public static void main(String[] args) {
		Resource resource = new ClassPathResource("spring-aop.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		//load 配置文件, 解析配置文件中的 <bean> 标签成 bd，然后
		// 注册到工厂
		reader.loadBeanDefinitions(resource);
		/**
		 * 配置文件
		 *
		 * <bean class="com.shengsiyuan.spring.lecture.aop.service.impl.MyServiceImpl" id="myService"/>
		 *
		 * 	<bean class="com.shengsiyuan.spring.lecture.aop.advisor.MyAdvisor" id="myAdvisor"/>
		 *
		 * 	<bean class="org.springframework.aop.framework.ProxyFactoryBean" id="myAop">
		 * 		<property name="proxyInterfaces">
		 * 			<value>com.shengsiyuan.spring.lecture.aop.service.MyService</value>
		 * 		</property>
		 * 		<property name="interceptorNames">
		 * 			<list>
		 * 				<value>myAdvisor</value>
		 * 			</list>
		 * 		</property>
		 * 		<property name="target">
		 * 			<ref bean="myService"/>
		 * 		</property>
		 * 	</bean>
		 *
		 * @see  org.springframework.aop.framework.ProxyFactoryBean
		 * @see  org.springframework.beans.factory.FactoryBean
		 *
		 * 问题：TODO
		 * 1.为什么获取beanName为 myAop 返回类 MyService 类型而不是一个 ProxyFactoryBean 类型的实例呢？
		 *
		 * 可以断点进去看看哦！！！！！
		 */
		MyService myService = (MyService) factory.getBean("myAop");
		myService.myMethod();

		System.out.println(myService.getClass());
		System.out.println(myService.getClass().getSuperclass());
		//[
		// interface com.shengsiyuan.spring.lecture.aop.service.MyService,
		// interface org.springframework.aop.SpringProxy,
		// interface org.springframework.aop.framework.Advised,
		// interface org.springframework.core.DecoratingProxy
		// ]
		System.out.println(Arrays.toString(myService.getClass().getInterfaces()));

		SpringProxy springProxy = (SpringProxy) myService;
	}
}