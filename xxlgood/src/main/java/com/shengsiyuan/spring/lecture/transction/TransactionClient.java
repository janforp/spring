package com.shengsiyuan.spring.lecture.transction;

import com.shengsiyuan.spring.lecture.transction.domain.Student;
import com.shengsiyuan.spring.lecture.transction.service.StudentService;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * https://www.jianshu.com/p/2f79ee33c8ad 事务
 *
 * @author zhucj
 * @see DataSourceTransactionManager#doCommit(org.springframework.transaction.support.DefaultTransactionStatus)
 * @since 20210325
 */
public class TransactionClient {

	public static void main(String[] args) {
		Resource resource = new ClassPathResource("spring-transaction.xml");
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		//load 配置文件, 解析配置文件中的 <bean> 标签成 bd，然后
		// 注册到工厂
		reader.loadBeanDefinitions(resource);

		/**
		 * * <!-- 通过 aop 实现事物	-->
		 * 		 * 	<bean id="studentServiceProxy" class="org.springframework.transaction.interceptor.TransactionProxyFactoryBean">
		 * 		 * 		<property name="target" ref="studentService"/>
		 * 		 * 		<property name="transactionManager" ref="transactionManager"/>
		 * 		 * 		<property name="transactionAttributes">
		 * 		 * 			<props>
		 * 		 * 				<prop key="save*">PROPAGATION_REQUIRED</prop>
		 * 		 * 				<prop key="update*">PROPAGATION_REQUIRED</prop>
		 * 		 * 				<prop key="remove*">PROPAGATION_REQUIRED</prop>
		 * 		 * 				<prop key="get*">PROPAGATION_REQUIRED,readOnly</prop>
		 * 		 *				{@link TransactionDefinition}
		 * 		 * 			</props>
		 * 		 * 		</property>
		 * 		 * 	</bean>
		 *
		 * 		 更加通用的配置方法！！！！！！！
		 *
		 * 	 * *   <bean id="baseTransactionProxy" class="org.springframework.transaction.interceptor.TransactionProxyFactoryBean"
		 * 	 * *        abstract="true">
		 * 	 * *      <property name="transactionManager" ref="transactionManager"/>
		 * 	 * *      <property name="transactionAttributes">
		 * 	 * *        <props>
		 * 	 * *          <prop key="insert*">PROPAGATION_REQUIRED</prop>
		 * 	 * *          <prop key="update*">PROPAGATION_REQUIRED</prop>
		 * 	 * *          <prop key="*">PROPAGATION_REQUIRED,readOnly</prop>
		 * 	 * *        </props>
		 * 	 * *      </property>
		 * 	 * *    </bean>
		 * 	 * *
		 * 	 * *    <bean id="myProxy" parent="baseTransactionProxy">
		 * 	 * *      <property name="target" ref="myTarget"/>
		 * 	 * *    </bean>
		 * 	 * *
		 * 	 * *    <bean id="yourProxy" parent="baseTransactionProxy">
		 * 	 * *      <property name="target" ref="yourTarget"/>
		 * 	 * *    </bean>
		 *
		 * @see TransactionProxyFactoryBean 事务代理工厂bean，通过它创建出来的对象，自然就具有事务的特性
		 * @see AbstractSingletonProxyFactoryBean#getObject() 拿到代理对象的方法
		 * @see AbstractSingletonProxyFactoryBean#afterPropertiesSet() 真正创建代理对象的方法是这个
		 *
		 * @see TransactionManager 标识接口
		 *
		 * @see PlatformTransactionManager 定义2个方法的接口
		 *
		 * @see DataSourceTransactionManager jdbc版本的事务管理器实现
		 * @see org.springframework.orm.hibernate5.HibernateTransactionManager Hibernate版本的实现
		 * @see 可以看mybatis的实现
		 * @see AbstractPlatformTransactionManager
		 * @see cn.com.servyou.xqy.framework.daoframework.transaction.MultipleDataSourcesTransactionManager 税友的实现
		 *
		 * @see TransactionAspectSupport#invokeWithinTransaction(java.lang.reflect.Method, java.lang.Class, org.springframework.transaction.interceptor.TransactionAspectSupport.InvocationCallback) 开启事务的方法
		 */
		//上面是解析配置文件，并且解析配置文件，生成 BeanDefinition 并且注册到工厂中
		StudentService service = (StudentService) factory.getBean("studentServiceProxy");

		Student student = new Student();
		student.setName("zhangSan");
		student.setAge(20);
		//底层：开启事务，执行，提交事务
		service.saveStudent(student);
	}
}