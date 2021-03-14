package com.shengsiyuan.spring.lecture;

import com.shengsiyuan.spring.lecture.annotation.Person;
import com.shengsiyuan.spring.lecture.annotation.PersonConfiguration;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;

/**
 * 基于注解跟基于xml的 bean 在创建时机上存在的唯一不同之处：
 * 1.基于XML配置的方式，Bean对象的创建是在程序首次从工厂中获取该对象的时候才创建
 * 2.基于注解的方法，Bean对象的创建是在注解处理器解析相应的 @Bean 注解时调用了该注解所修饰的方法，当该方法执行后，
 * 相应的对象自然就已经被创建出来了，这时，spring就会将对象纳入到工厂的管理范围之内，当我们首次从工厂获取该bean对象的时候，该bean
 * 对象实际上已经完成了创建并且被纳入工厂管理服务之内
 *
 * @author zhucj
 * @since 20210325
 */
public class AnnotationIocSpringClient {

	public static void main(String[] args) {

		/**
		 * 基于注解的ioc工厂
		 * 对应基于 xml的步骤为：
		 * DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		 */
		AnnotationConfigApplicationContext annotationIoc
				/**
				 * @see AnnotationConfigUtils#registerAnnotationConfigProcessors(org.springframework.beans.factory.support.BeanDefinitionRegistry, java.lang.Object)
				 */
				= new AnnotationConfigApplicationContext();

		/**
		 * 注册
		 *
		 * Resource resource = new ClassPathResource("spring-construct-property.xml");
		 * XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		 * reader.loadBeanDefinitions(resource);
		 *
		 * @see AnnotatedBeanDefinitionReader#register(java.lang.Class[]) 工厂是委托给该实例进行注册的
		 */
		annotationIoc.register(PersonConfiguration.class);

		System.out.println("************* refresh 方法执行 开始");

		/**
		 * 刷新,具体
		 * 1.对所有bean定义的解析
		 * 2.完成所有注解的分析
		 * 3.把 @Bean 标注的方法解析成一个 bd 注册到ioc
		 * 4.调用{@link PersonConfiguration#getPerson()}方法完成bean的实例化
		 */
		annotationIoc.refresh();

		System.out.println("************* refresh 方法执行 结束");

		PersonConfiguration personConfiguration = (PersonConfiguration) annotationIoc.getBean("personConfiguration");
		Person person = (Person) annotationIoc.getBean("person");

		/**
		 * PersonConfiguration 的类型为
		 *
		 * class com.shengsiyuan.spring.lecture.annotation.PersonConfiguration$$EnhancerBySpringCGLIB$$fd26128
		 *
		 * 它是被 cglib 代理过的？为什么呢？TODO
		 */
		System.out.println("PersonConfiguration 的类型为 " + personConfiguration.getClass());

		//Person 的类型为 class com.shengsiyuan.spring.lecture.annotation.Person
		System.out.println("Person 的类型为 " + person.getClass());

		System.out.println(person.getId() + " 名称为 " + person.getName());
	}
}