package com.shengsiyuan.spring.lecture;

import com.javaxxl.bpp.Student;
import com.javaxxl.bpp.Teacher;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * SpringClient
 *
 * @author zhucj
 * @since 20210325
 */
public class SpringClient {

	/**
	 * 关于Spring Bean实例(bd)的注册流程
	 * 1.定义好Spring配置文件
	 * 2.通过Resource对象将Spring配置文件进行抽象，抽象成一个具体的Resource对象，（如ClassPathResource)
	 * 3.定义好将要使用的Bean工厂（各种BeanFactory）
	 * 4.定义好XmlBeanDefinitionReader对象，并将工厂作为参数传进去，从而构建好二者直接的关联关系
	 * 5.通过 XmlBeanDefinitionReader 对象读取之前所抽取出的Resource对象
	 * 6.解析流程开始
	 * 7.针对XML文件进行各种元素以及元素属性的解析，在这里，真正的解析是通过 BeanDefinitionParserDelegate 对象来完成的（代理模式）
	 * 8.通过 BeanDefinitionParserDelegate 对象在解析 XML 文件时，又使用了模版方法设计模式（pre,process,post）
	 * 9.当所有 Bean 标签元素都解析完毕后，开始定义一个 BeanDefinition 对象，该对象是一个非常重要的对象，里面容纳了一个 Bean 相关的所有属性
	 * 10.BeanDefinition 对象创建完成后，Spring 又会创建一个 BeanDefinitionHolder 对象来持有这个 BeanDefinition 对象
	 * 11.BeanDefinitionHolder 对象主要包含3 部分内容，beanName,alias,bd
	 * 12.工厂将解析出来的 Bean 信息存放在一个 并发Map 中，该Map的 beanName 唯一，值为 bd 对象
	 * 13.调用 Bean  解析完毕触发动作，从而触发相应的监听器的方法执行（观察者模式）
	 */
	public static void main(String[] args) {
		Resource resource = new ClassPathResource("spring-construct-property.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		//load 配置文件, 解析配置文件中的 <bean> 标签成 bd，然后
		// 注册到工厂
		reader.loadBeanDefinitions(resource);

		//上面是解析配置文件，并且解析配置文件，生成 BeanDefinition 并且注册到工厂中
		Teacher teacher = (Teacher) factory.getBean("teacher");
		System.out.println(teacher);

		Student service = (Student) factory.getBean("student");
		System.out.println(service);
	}
}