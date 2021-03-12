package com.shengsiyuan.spring.lecture.transction;

import com.shengsiyuan.spring.lecture.transction.domain.Student;
import com.shengsiyuan.spring.lecture.transction.service.StudentService;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * TransctionClient
 *
 * @author zhucj
 * @since 20210325
 */
public class TransactionClient {

	public static void main(String[] args) {
		Resource resource = new ClassPathResource("spring-transction.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		//load 配置文件, 解析配置文件中的 <bean> 标签成 bd，然后
		// 注册到工厂
		reader.loadBeanDefinitions(resource);

		//上面是解析配置文件，并且解析配置文件，生成 BeanDefinition 并且注册到工厂中
		StudentService service = (StudentService) factory.getBean("studentServiceProxy");

		Student student = new Student();
		student.setName("zhangsan");
		student.setAge(20);

		service.saveStudent(student);
	}
}
