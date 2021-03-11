package com.shengsiyuan.spring.lecture.aop;

import com.shengsiyuan.spring.lecture.aop.service.MyService;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanFactory;
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

	/**
	 * TODO 很重要！！！
	 *
	 * 如果Bean实现此接口，则它将用作对象公开的工厂，而不是直接用作将自身公开的Bean实例。
	 *
	 * FactoryBean 与 {@link BeanFactory} 的区别？!!!!!!
	 *
	 * 1.BeanFactory 是 Spring 的 IOC 工厂，它里面管理着Spring说创建出来的各种Bean对象，当我们在配置文件(注解)中声明了某个Bean的Id后，通过
	 * 这个id就能获取到与该id所对应的class对象实例（可能新建，可能从缓存获取）
	 * 2.FactoryBean本质上也是一个bean，它同其他bean一样，也是由BeanFactory所管理和维护，当然他的实例也会缓存到spring工厂(BeanFactory)中（如果是单例），
	 * 他与普通bean的唯一区别在于，当spring创建另一个FactoryBean实例后，他接下来会判断当前所创建的Bean是否是一个FactoryBean实例，如果不是，那么就直接将
	 * 创建出来的bean返回给客户端，如果是，那么他会对其进行进一步的处理，根据配置文件所配置的target,advisor,interfaces等信息，在运行期动态的构建出一个类，并且
	 * 生成该类的一个实例，最后将该实例返回给客户端，因此，我们在声明一个FactoryBean的时候，通过id获取的并非是这个FactoryBean的实例，而是他动态生成出来的一个代理对象
	 *
	 * @see org.springframework.aop.framework.ProxyFactoryBean
	 */
	@SuppressWarnings("rawtypes")
	public static void main(String[] args) throws Exception {
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
		 * @see AbstractBeanFactory#getObjectForBeanInstance 因为 FactoryBean 可以管理一个 bean，但是他本身也是一个bean
		 * @see ProxyFactoryBean#getObject() FactoryBean 获取内部管理的bean的方法!!
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

		FactoryBean factoryBean = (FactoryBean) factory.getBean("&myAop");
		MyService object = (MyService) factoryBean.getObject();
		object.myMethod();
	}
}