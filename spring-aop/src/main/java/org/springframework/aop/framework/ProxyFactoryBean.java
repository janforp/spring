package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link org.springframework.beans.factory.FactoryBean} implementation that builds an
 * AOP proxy based on beans in Spring {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>{@link org.aopalliance.intercept.MethodInterceptor MethodInterceptors} and
 * {@link org.springframework.aop.Advisor Advisors} are identified by a list of bean
 * names in the current bean factory, specified through the "interceptorNames" property.
 * The last entry in the list can be the name of a target bean or a
 * {@link org.springframework.aop.TargetSource}; however, it is normally preferable
 * to use the "targetName"/"target"/"targetSource" properties instead.
 *
 * <p>Global interceptors and advisors can be added at the factory level. The specified
 * ones are expanded in an interceptor list where an "xxx*" entry is included in the
 * list, matching the given prefix with the bean names (e.g. "global*" would match
 * both "globalBean1" and "globalBean2", "*" all defined interceptors). The matching
 * interceptors get applied according to their returned order value, if they implement
 * the {@link org.springframework.core.Ordered} interface.
 *
 * <p>Creates a JDK proxy when proxy interfaces are given, and a CGLIB proxy for the
 * actual target class if not. Note that the latter will only work if the target class
 * does not have final methods, as a dynamic subclass will be created at runtime.
 *
 * <p>It's possible to cast a proxy obtained from this factory to {@link Advised},
 * or to obtain the ProxyFactoryBean reference and programmatically manipulate it.
 * This won't work for existing prototype references, which are independent. However,
 * it will work for prototypes subsequently obtained from the factory. Changes to
 * interception will work immediately on singletons (including existing references).
 * However, to change interfaces or target it's necessary to obtain a new instance
 * from the factory. This means that singleton instances obtained from the factory
 * do not have the same object identity. However, they do have the same interceptors
 * and target, and changing any reference will change all objects.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setInterceptorNames
 * @see #setProxyInterfaces
 * @see org.aopalliance.intercept.MethodInterceptor
 * @see org.springframework.aop.Advisor
 * @see Advised
 */
@SuppressWarnings("serial")
public class ProxyFactoryBean

		/**
		 * 同时该类型的实例还可以创建AOP代理对象
		 *
		 * @see AdvisedSupport 最终该类初始化的拦截器链表就会存入该类的字段中
		 * @see AdvisedSupport#advisors 拦截器链表
		 */
		extends ProxyCreatorSupport

		/**
		 * 不一个普通bean
		 */
		implements FactoryBean<Object>,

		BeanClassLoaderAware, BeanFactoryAware {

	/**
	 * TODO 非常重要！！！
	 *
	 * ProxyFactoryBean 的构成
	 * 1.target：目标对象，需要对其进行切面增强
	 *
	 * 2.proxyInterfaces:代理对象所实现的接口
	 *
	 * 3.interceptorNames:通知器(Advisor)列表，通知器中包含了通知(Advice)与切点(Pointcut)
	 * @see Advisor
	 * @see org.springframework.aop.Pointcut
	 * @see Advice
	 *
	 * ProxyFactoryBean 的作用
	 * 总的来说， ProxyFactoryBean 的作用可以用下面这句话概括：
	 * 针对目标对象来创建代理对象，将对目标对象方法的调用转到对相应代理对象方法的调用，
	 * 并且可以在代理对象方法调用前后执行与之匹配的各个通知器(Advisor)中定义好的方法！
	 *
	 * 对客户端来看，他还是操作之前的被代理对象，但是实际上方法会调用到代理对象！
	 */

	/**
	 * TODO ????
	 * This suffix in a value in an interceptor list indicates to expand globals.
	 */
	public static final String GLOBAL_SUFFIX = "*";

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private String[] interceptorNames;

	@Nullable
	private String targetName;

	private boolean autodetectInterfaces = true;

	private boolean singleton = true;

	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	private boolean freezeProxy = false;

	@Nullable
	private transient ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private transient boolean classLoaderConfigured = false;

	@Nullable
	private transient BeanFactory beanFactory;

	/**
	 * Whether the advisor chain has already been initialized.
	 *
	 * advisor 链是否已初始化。
	 *
	 * 拦截器链是否已经初始化过，保证只初始化一次！
	 *
	 * @see ProxyFactoryBean#initializeAdvisorChain() 初始化拦截器链的逻辑
	 * @see AdvisedSupport#advisors 初始化之后的拦截器都会放到这个链表中
	 */
	private boolean advisorChainInitialized = false;

	/**
	 * If this is a singleton, the cached singleton proxy instance.
	 * -- 如果这是一个单例，则为缓存的单例代理实例。
	 *
	 * 缓存的是该 FactoryBean 管理的对象
	 */
	@Nullable
	private Object singletonInstance;

	/**
	 * Set the names of the interfaces we're proxying. If no interface
	 * is given, a CGLIB for the actual class will be created.
	 * <p>This is essentially equivalent to the "setInterfaces" method,
	 * but mirrors TransactionProxyFactoryBean's "setProxyInterfaces".
	 *
	 * @see #setInterfaces
	 * @see AbstractSingletonProxyFactoryBean#setProxyInterfaces
	 */
	public void setProxyInterfaces(Class<?>[] proxyInterfaces) {
		setInterfaces(proxyInterfaces);
	}

	/**
	 * Set the list of Advice/Advisor bean names. This must always be set
	 * to use this factory bean in a bean factory.
	 * <p>The referenced beans should be of type Interceptor, Advisor or Advice
	 * The last entry in the list can be the name of any bean in the factory.
	 * If it's neither an Advice nor an Advisor, a new SingletonTargetSource
	 * is added to wrap it. Such a target bean cannot be used if the "target"
	 * or "targetSource" or "targetName" property is set, in which case the
	 * "interceptorNames" array must contain only Advice/Advisor bean names.
	 * <p><b>NOTE: Specifying a target bean as final name in the "interceptorNames"
	 * list is deprecated and will be removed in a future Spring version.</b>
	 * Use the {@link #setTargetName "targetName"} property instead.
	 *
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see org.springframework.aop.Advisor
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.target.SingletonTargetSource
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set the name of the target bean. This is an alternative to specifying
	 * the target name at the end of the "interceptorNames" array.
	 * <p>You can also specify a target object or a TargetSource object
	 * directly, via the "target"/"targetSource" property, respectively.
	 *
	 * @see #setInterceptorNames(String[])
	 * @see #setTarget(Object)
	 * @see #setTargetSource(org.springframework.aop.TargetSource)
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	/**
	 * Set whether to autodetect proxy interfaces if none specified.
	 * <p>Default is "true". Turn this flag off to create a CGLIB
	 * proxy for the full target class if no interfaces specified.
	 *
	 * @see #setProxyTargetClass
	 */
	public void setAutodetectInterfaces(boolean autodetectInterfaces) {
		this.autodetectInterfaces = autodetectInterfaces;
	}

	/**
	 * Set the value of the singleton property. Governs whether this factory
	 * should always return the same proxy instance (which implies the same target)
	 * or whether it should return a new prototype instance, which implies that
	 * the target and interceptors may be new instances also, if they are obtained
	 * from prototype bean definitions. This allows for fine control of
	 * independence/uniqueness in the object graph.
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	/**
	 * Specify the AdvisorAdapterRegistry to use.
	 * Default is the global AdvisorAdapterRegistry.
	 *
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	/**
	 * Set the ClassLoader to generate the proxy class in.
	 * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the
	 * containing BeanFactory for loading all bean classes. This can be
	 * overridden here for specific proxies.
	 */
	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		checkInterceptorNames();
	}

	/**
	 * 关键方法！！
	 *
	 * * 配置文件
	 * *
	 * * <bean class="com.shengsiyuan.spring.lecture.aop.service.impl.MyServiceImpl" id="myService"/>
	 * *
	 * * 	<bean class="com.shengsiyuan.spring.lecture.aop.advisor.MyAdvisor" id="myAdvisor"/>
	 * *
	 * * 	<bean class="org.springframework.aop.framework.ProxyFactoryBean" id="myAop">
	 * * 		<property name="proxyInterfaces">
	 * * 			<value>com.shengsiyuan.spring.lecture.aop.service.MyService</value>
	 * * 		</property>
	 * * 		<property name="interceptorNames">
	 * * 			<list>
	 * * 				<value>myAdvisor</value>
	 * * 			</list>
	 * * 		</property>
	 * * 		<property name="target">
	 * * 			<ref bean="myService"/>
	 * * 		</property>
	 * * 	</bean>
	 * *
	 * * @see  org.springframework.aop.framework.ProxyFactoryBean
	 * * @see  org.springframework.beans.factory.FactoryBean
	 *
	 * Return a proxy. Invoked when clients obtain beans from this factory bean. -- 返回代理。当客户从该工厂bean获得bean时调用。
	 * Create an instance of the AOP proxy to be returned by this factory. -- 创建要由该工厂返回的AOP代理的实例。
	 * The instance will be cached for a singleton, and create on each call to
	 * {@code getObject()} for a proxy. -- 该实例将被缓存一个单例，并在每次调用{@code getObject（）}时创建一个代理。
	 *
	 * @return a fresh AOP proxy reflecting the current state of this factory
	 */
	@Override
	@Nullable
	public Object getObject() throws BeansException {
		/**
		 *初始化拦截器链的逻辑
		 * @see AdvisedSupport 最终该类初始化的拦截器链表就会存入该类的字段中
		 * @see AdvisedSupport#advisors 拦截器链表
		 */
		initializeAdvisorChain();
		if (isSingleton()) {
			//该 FactoryBean 是单例的
			return getSingletonInstance();
		} else {
			//该 FactoryBean 不是单例的
			if (this.targetName == null) {
				logger.info("Using non-singleton proxies with singleton targets is often undesirable. " + "Enable prototype proxies by setting the 'targetName' property.");
			}
			//创建一个原型模式的实例
			return newPrototypeInstance();
		}
	}

	/**
	 * Return the type of the proxy. Will check the singleton instance if
	 * already created, else fall back to the proxy interface (in case of just
	 * a single one), the target bean type, or the TargetSource's target class.
	 *
	 * @see org.springframework.aop.TargetSource#getTargetClass
	 */
	@Override
	public Class<?> getObjectType() {
		synchronized (this) {
			if (this.singletonInstance != null) {
				return this.singletonInstance.getClass();
			}
		}
		Class<?>[] ifcs = getProxiedInterfaces();
		if (ifcs.length == 1) {
			return ifcs[0];
		} else if (ifcs.length > 1) {
			return createCompositeInterface(ifcs);
		} else if (this.targetName != null && this.beanFactory != null) {
			return this.beanFactory.getType(this.targetName);
		} else {
			return getTargetClass();
		}
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	/**
	 * Create a composite interface Class for the given interfaces,
	 * implementing the given interfaces in one single Class.
	 * <p>The default implementation builds a JDK proxy class for the
	 * given interfaces.
	 *
	 * @param interfaces the interfaces to merge
	 * @return the merged interface as Class
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
		return ClassUtils.createCompositeInterface(interfaces, this.proxyClassLoader);
	}

	/**
	 * Return the singleton instance of this class's proxy object,
	 * lazily creating it if it hasn't been created already.
	 * -- 返回此类的 代理对象！！！！ 的单例实例，如果尚未创建，则延迟创建它。
	 *
	 * @return the shared singleton proxy
	 */
	private synchronized Object getSingletonInstance() {
		if (this.singletonInstance == null) {
			//缓存没命中！

			//从ioc工厂拿到被代理的实例，并且赋值
			this.targetSource = freshTargetSource();

			if (this.autodetectInterfaces //字段探测接口，一般为 true

					&& getProxiedInterfaces().length == 0 //被代理接口的数量为0，意思就是配置的是没有指定

					/**
					 * @see ProxyConfig#proxyTargetClass 如果为true,则使用cglib，否则使用jdk动态代理
					 */
					&& !isProxyTargetClass()) {
				// Rely on AOP infrastructure to tell us what interfaces to proxy.-- 依靠AOP基础结构告诉我们代理的接口。
				Class<?> targetClass = getTargetClass();//获取目标对象的类
				if (targetClass == null) {
					throw new FactoryBeanNotInitializedException("Cannot determine target class for proxy");
				}
				/**
				 * 设置要代理的接口！
				 */
				setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
			// Initialize the shared singleton instance.
			super.setFrozen(this.freezeProxy);

			//生成代理对象并且缓存到 singletonInstance 字段
			this.singletonInstance =
					/**
					 * 使用jdk动态代理或者cglib代理生成代理对象
					 */
					getProxy(
							/**
							 * 获取代理实例生成的方式
							 */
							createAopProxy()
					);
		}

		//以后再次getObject的时候就使用该缓存类
		return this.singletonInstance;
	}

	/**
	 * Create a new prototype instance of this class's created proxy object,
	 * backed by an independent AdvisedSupport configuration.
	 *
	 * -- 在独立的AdvisedSupport配置支持下，创建此类的已创建代理对象的新原型实例。
	 *
	 * @return a totally independent proxy, whose advice we may manipulate in isolation
	 */
	private synchronized Object newPrototypeInstance() {
		// In the case of a prototype, we need to give the proxy
		// an independent instance of the configuration.
		// In this case, no proxy will have an instance of this object's configuration,
		// but will have an independent copy.
		ProxyCreatorSupport copy = new ProxyCreatorSupport(getAopProxyFactory());

		// The copy needs a fresh advisor chain, and a fresh TargetSource.
		TargetSource targetSource = freshTargetSource();
		copy.copyConfigurationFrom(this, targetSource, freshAdvisorChain());
		if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
			// Rely on AOP infrastructure to tell us what interfaces to proxy.
			Class<?> targetClass = targetSource.getTargetClass();
			if (targetClass != null) {
				copy.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
		}
		copy.setFrozen(this.freezeProxy);

		return getProxy(copy.createAopProxy());
	}

	/**
	 * Return the proxy object to expose.
	 * <p>The default implementation uses a {@code getProxy} call with
	 * the factory's bean class loader. Can be overridden to specify a
	 * custom class loader.
	 *
	 * @param aopProxy the prepared AopProxy instance to get the proxy from
	 * @return the proxy object to expose
	 * @see AopProxy#getProxy(ClassLoader)
	 */
	protected Object getProxy(AopProxy aopProxy) {
		return aopProxy.getProxy(this.proxyClassLoader);
	}

	/**
	 * Check the interceptorNames list whether it contains a target name as final element.
	 * If found, remove the final name from the list and set it as targetName.
	 */
	private void checkInterceptorNames() {
		if (!ObjectUtils.isEmpty(this.interceptorNames)) {
			String finalName = this.interceptorNames[this.interceptorNames.length - 1];
			if (this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				// The last name in the chain may be an Advisor/Advice or a target/TargetSource.
				// Unfortunately we don't know; we must look at type of the bean.
				if (!finalName.endsWith(GLOBAL_SUFFIX) && !isNamedBeanAnAdvisorOrAdvice(finalName)) {
					// The target isn't an interceptor.
					this.targetName = finalName;
					if (logger.isDebugEnabled()) {
						logger.debug("Bean with name '" + finalName + "' concluding interceptor chain " +
								"is not an advisor class: treating it as a target or TargetSource");
					}
					this.interceptorNames = Arrays.copyOf(this.interceptorNames, this.interceptorNames.length - 1);
				}
			}
		}
	}

	/**
	 * Look at bean factory metadata to work out whether this bean name,
	 * which concludes the interceptorNames list, is an Advisor or Advice,
	 * or may be a target.
	 *
	 * @param beanName bean name to check
	 * @return {@code true} if it's an Advisor or Advice
	 */
	private boolean isNamedBeanAnAdvisorOrAdvice(String beanName) {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		Class<?> namedBeanClass = this.beanFactory.getType(beanName);
		if (namedBeanClass != null) {
			return (Advisor.class.isAssignableFrom(namedBeanClass) || Advice.class.isAssignableFrom(namedBeanClass));
		}
		// Treat it as an target bean if we can't tell.
		if (logger.isDebugEnabled()) {
			logger.debug("Could not determine type of bean with name '" + beanName +
					"' - assuming it is neither an Advisor nor an Advice");
		}
		return false;
	}

	/**
	 * * 配置文件
	 * *
	 * * <bean class="com.shengsiyuan.spring.lecture.aop.service.impl.MyServiceImpl" id="myService"/>
	 * *
	 * * 	<bean class="com.shengsiyuan.spring.lecture.aop.advisor.MyAdvisor" id="myAdvisor"/>
	 * *
	 * * 	<bean class="org.springframework.aop.framework.ProxyFactoryBean" id="myAop">
	 * * 		<property name="proxyInterfaces">
	 * * 			<value>com.shengsiyuan.spring.lecture.aop.service.MyService</value>
	 * * 		</property>
	 * * 		<property name="interceptorNames">
	 * * 			<list>
	 * * 				<value>myAdvisor</value> TODO 这个方法就是初始化这个配置的拦截器链表
	 * * 			</list>
	 * * 		</property>
	 * * 		<property name="target">
	 * * 			<ref bean="myService"/>
	 * * 		</property>
	 * * 	</bean>
	 *
	 * Create the advisor (interceptor) chain. Advisors that are sourced
	 * from a BeanFactory will be refreshed each time a new prototype instance
	 * is added. Interceptors added programmatically through the factory API
	 * are unaffected by such changes.
	 */
	private synchronized void initializeAdvisorChain() throws AopConfigException, BeansException {
		if (this.advisorChainInitialized) {
			//拦截器链是否已经初始化过，保证只初始化一次！
			return;
		}

		if (!ObjectUtils.isEmpty(this.interceptorNames)) {
			if (this.beanFactory == null) {
				//没有ioc工厂，直接报错
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " + "- cannot resolve interceptor names " + Arrays.asList(this.interceptorNames));
			}

			// Globals can't be last unless we specified a targetSource using the property...
			// 除非我们使用属性指定了targetSource，否则全局变量不能为最后一个。
			if (
			/**
			 * 如果 interceptorNames 链的最后一个的名称是 * 结尾！
			 */
					this.interceptorNames[this.interceptorNames.length - 1].endsWith(GLOBAL_SUFFIX)
							&&
							/**
							 * 并且 targetName 为 null
							 */
							this.targetName == null
							&&
							/**
							 * 并且 targetSource 也是默认的空实例 {@link SingletonTargetSource} 一般是这个类型的实例，一般该字段都会有值的
							 */
							this.targetSource == EMPTY_TARGET_SOURCE) {

				throw new AopConfigException("Target required after globals");
			}

			/**
			 *  Materialize(物化) interceptor chain from bean names.-- 从bean名称中实现拦截器链。
			 *
			 * <property name="interceptorNames">
			 * 	<list>
			 * 		<value>myAdvisor</value> TODO 这个方法就是初始化这个配置的拦截器链表
			 * 	</list>
			 * </property>
			 */
			for (String name : this.interceptorNames) {
				if (name.endsWith(GLOBAL_SUFFIX)) {
					//进入该分支说明：当前 name 是 * 结尾的，如：teacher*
					if (!(this.beanFactory instanceof ListableBeanFactory)) {
						//如果名称是 * 结尾，并且当前 ioc 工厂并不是 ListableBeanFactory 的实现类，则报错！
						throw new AopConfigException("Can only use global advisors or interceptors with a ListableBeanFactory");
					}
					//如果名称是 * 结尾，并且当前 ioc 工厂是 ListableBeanFactory 的实现类
					addGlobalAdvisors((ListableBeanFactory) this.beanFactory, name.substring(0, name.length() - GLOBAL_SUFFIX.length()));
				}

				//当前循环的name不是 GLOBAL_SUFFIX 结尾
				else {
					//当前循环的name不是 GLOBAL_SUFFIX 结尾

					// If we get here, we need to add a named interceptor.
					// We must check if it's a singleton or prototype.
					// 如果到达这里，我们需要添加一个命名拦截器。我们必须检查它是单例还是原型。
					Object advice;
					if (this.singleton || this.beanFactory.isSingleton(name)) {
						// Add the real Advisor/Advice to the chain. -- 将真正的Advisor / Advice添加到链中。
						/**
						 * 如果是单例，直接拿到单实例
						 */
						advice = this.beanFactory.getBean(name);
					} else {
						// It's a prototype Advice or Advisor: replace with a prototype.
						// Avoid unnecessary creation of prototype bean just for advisor chain initialization.
						// 避免仅用于顾问程序链初始化的不必要的原型bean创建。
						/**
						 * 如果是原型模式，则只是封装下，并没有实例化
						 */
						advice = new PrototypePlaceholderAdvisor(name);
					}
					addAdvisorOnChainCreation(advice);//把该拦截器放到拦截器链表中
				}
			}
		}

		/**
		 * 拦截器链已经初始化过了！！！
		 */
		this.advisorChainInitialized = true;
	}

	/**
	 * Return an independent advisor chain.
	 * We need to do this every time a new prototype instance is returned,
	 * to return distinct instances of prototype Advisors and Advices.
	 */
	private List<Advisor> freshAdvisorChain() {
		Advisor[] advisors = getAdvisors();
		List<Advisor> freshAdvisors = new ArrayList<>(advisors.length);
		for (Advisor advisor : advisors) {
			if (advisor instanceof PrototypePlaceholderAdvisor) {
				PrototypePlaceholderAdvisor pa = (PrototypePlaceholderAdvisor) advisor;
				if (logger.isDebugEnabled()) {
					logger.debug("Refreshing bean named '" + pa.getBeanName() + "'");
				}
				// Replace the placeholder with a fresh prototype instance resulting from a getBean lookup
				if (this.beanFactory == null) {
					throw new IllegalStateException("No BeanFactory available anymore (probably due to " +
							"serialization) - cannot resolve prototype advisor '" + pa.getBeanName() + "'");
				}
				Object bean = this.beanFactory.getBean(pa.getBeanName());
				Advisor refreshedAdvisor = namedBeanToAdvisor(bean);
				freshAdvisors.add(refreshedAdvisor);
			} else {
				// Add the shared instance.
				freshAdvisors.add(advisor);
			}
		}
		return freshAdvisors;
	}

	/**
	 * Add all global interceptors and pointcuts. -- 添加所有全局拦截器和切入点。
	 */
	private void addGlobalAdvisors(ListableBeanFactory beanFactory, String prefix) {
		String[] globalAdvisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Advisor.class);
		String[] globalInterceptorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Interceptor.class);
		if (globalAdvisorNames.length > 0 || globalInterceptorNames.length > 0) {
			List<Object> beans = new ArrayList<>(globalAdvisorNames.length + globalInterceptorNames.length);
			for (String name : globalAdvisorNames) {
				if (name.startsWith(prefix)) {
					beans.add(beanFactory.getBean(name));
				}
			}
			for (String name : globalInterceptorNames) {
				if (name.startsWith(prefix)) {
					beans.add(beanFactory.getBean(name));
				}
			}
			AnnotationAwareOrderComparator.sort(beans);
			for (Object bean : beans) {
				addAdvisorOnChainCreation(bean);
			}
		}
	}

	/**
	 * 在创建拦截器链的时候添加一个拦截器
	 *
	 * Invoked when advice chain is created.
	 * <p>
	 * Add the given advice, advisor or object to the interceptor list.-- 将给定的建议，顾问或对象添加到拦截器列表中。
	 * Because of these three possibilities, we can't type the signature more strongly.
	 *
	 * @param next advice, advisor or target object
	 */
	private void addAdvisorOnChainCreation(Object next) {
		// We need to convert to an Advisor if necessary so that our source reference
		// matches what we find from superclass interceptors.
		addAdvisor(
				/**
				 * 检查拦截器类型，确保添加到拦截器链表的是一个 Advice/Advisor 类型
				 * @see DefaultAdvisorAdapterRegistry#wrap(java.lang.Object)
				 */
				namedBeanToAdvisor(next)
		);
	}

	/**
	 * Return a TargetSource to use when creating a proxy. If the target was not
	 * specified at the end of the interceptorNames list, the TargetSource will be
	 * this class's TargetSource member. Otherwise, we get the target bean and wrap
	 * it in a TargetSource if necessary.
	 */
	private TargetSource freshTargetSource() {
		if (this.targetName == null) {
			// Not refreshing target: bean name not specified in 'interceptorNames'
			return this.targetSource;
		} else {
			//配置的被代理实例是一个 beanName ，则需要创建该 bean实例
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve target with name '" + this.targetName + "'");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Refreshing target with name '" + this.targetName + "'");
			}
			//需要创建该 bean实例
			Object target = this.beanFactory.getBean(this.targetName);
			return (target instanceof TargetSource ?
					(TargetSource) target

					/**
					 * 最终还是封装到 SingletonTargetSource
					 */
					: new SingletonTargetSource(target));
		}
	}

	/**
	 * Convert the following object sourced from calling getBean() on a name in the
	 * interceptorNames array to an Advisor or TargetSource.
	 * -- 将以下对象（从对interceptorNames数组中的名称的调用getBean（）所获得的源转换为Advisor或TargetSource）。
	 */
	private Advisor namedBeanToAdvisor(Object next) {
		try {
			/**@see DefaultAdvisorAdapterRegistry#wrap(java.lang.Object)***/
			return this.advisorAdapterRegistry.wrap(next);
		} catch (UnknownAdviceTypeException ex) {
			// We expected this to be an Advisor or Advice,
			// but it wasn't. This is a configuration error.
			throw new AopConfigException("Unknown advisor type " + next.getClass() +
					"; can only include Advisor or Advice type beans in interceptorNames chain " +
					"except for last entry which may also be target instance or TargetSource", ex);
		}
	}

	/**
	 * Blow away and recache singleton on an advice change.
	 */
	@Override
	protected void adviceChanged() {
		super.adviceChanged();
		if (this.singleton) {
			logger.debug("Advice has changed; re-caching singleton instance");
			synchronized (this) {
				this.singletonInstance = null;
			}
		}
	}

	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
	}

	/**
	 * Used in the interceptor chain where we need to replace a bean with a prototype on creating a proxy.
	 * -- 在拦截器链中使用，在该链中我们需要在创建代理时用原型替换Bean。
	 */
	private static class PrototypePlaceholderAdvisor implements Advisor, Serializable {

		private final String beanName;

		private final String message;

		public PrototypePlaceholderAdvisor(String beanName) {
			this.beanName = beanName;
			this.message = "Placeholder for prototype Advisor/Advice with bean name '" + beanName + "'";
		}

		public String getBeanName() {
			return this.beanName;
		}

		@Override
		public Advice getAdvice() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public boolean isPerInstance() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public String toString() {
			return this.message;
		}
	}
}