package org.springframework.beans.factory.support;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.log.LogMessage;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Abstract base class for {@link org.springframework.beans.factory.BeanFactory}
 * implementations, providing the full capabilities of the
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} SPI.
 * Does <i>not</i> assume a listable bean factory: can therefore also be used
 * as base class for bean factory implementations which obtain bean definitions
 * from some backend resource (where bean definition access is an expensive operation).
 *
 * <p>This class provides a singleton cache (through its base class
 * {@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry},
 * singleton/prototype determination, {@link org.springframework.beans.factory.FactoryBean}
 * handling, aliases, bean definition merging for child bean definitions,
 * and bean destruction ({@link org.springframework.beans.factory.DisposableBean}
 * interface, custom destroy methods). Furthermore, it can manage a bean factory
 * hierarchy (delegating to the parent in case of an unknown bean), through implementing
 * the {@link org.springframework.beans.factory.HierarchicalBeanFactory} interface.
 *
 * <p>The main template methods to be implemented by subclasses are
 * {@link #getBeanDefinition} and {@link #createBean}, retrieving a bean definition
 * for a given bean name and creating a bean instance for a given bean definition,
 * respectively. Default implementations of those operations can be found in
 * {@link DefaultListableBeanFactory} and {@link AbstractAutowireCapableBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @see #getBeanDefinition
 * @see #createBean
 * @see AbstractAutowireCapableBeanFactory#createBean
 * @see DefaultListableBeanFactory#getBeanDefinition
 * @since 15 April 2001
 */
@SuppressWarnings("all")
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

	/**
	 * Parent bean factory, for bean inheritance support.
	 */
	@Nullable
	private BeanFactory parentBeanFactory;

	/**
	 * ClassLoader to resolve bean class names with, if necessary.
	 */
	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * ClassLoader to temporarily resolve bean class names with, if necessary.
	 */
	@Nullable
	private ClassLoader tempClassLoader;

	/**
	 * Whether to cache bean metadata or rather reobtain it for every access.
	 */
	private boolean cacheBeanMetadata = true;

	/**
	 * Resolution strategy for expressions in bean definition values.
	 *
	 * @see org.springframework.context.support.AbstractApplicationContext#shouldIgnoreSpel
	 */
	@Nullable
	private BeanExpressionResolver beanExpressionResolver;

	/**
	 * Spring ConversionService to use instead of PropertyEditors.
	 */
	@Nullable
	private ConversionService conversionService;

	/**
	 * Custom PropertyEditorRegistrars to apply to the beans of this factory.
	 */
	private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);

	/**
	 * Custom PropertyEditors to apply to the beans of this factory.
	 */
	private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);

	/**
	 * A custom TypeConverter to use, overriding the default PropertyEditor mechanism.
	 */
	@Nullable
	private TypeConverter typeConverter;

	/**
	 * String resolvers to apply e.g. to annotation attribute values.
	 */
	private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();

	/**
	 * BeanPostProcessors to apply.
	 *
	 * @see org.springframework.context.support.PostProcessorRegistrationDelegate#registerBeanPostProcessors(org.springframework.beans.factory.config.ConfigurableListableBeanFactory, java.util.List) 该方法向该集合中添加了bpp
	 */
	private final List<BeanPostProcessor> beanPostProcessors = new BeanPostProcessorCacheAwareList();

	/**
	 * Cache of pre-filtered post-processors.
	 */
	@Nullable
	private volatile BeanPostProcessorCache beanPostProcessorCache;

	/**
	 * Map from scope identifier String to corresponding Scope.
	 */
	private final Map<String, Scope> scopes = new LinkedHashMap<>(8);

	/**
	 * Security context used when running with a SecurityManager.
	 */
	@Nullable
	private SecurityContextProvider securityContextProvider;

	/**
	 * Map from bean name to merged RootBeanDefinition.
	 * 合并bd 缓存
	 */
	private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);

	/**
	 * Names of beans that have already been created at least once.
	 * -- 至少已经创建一次的bean的名称。
	 */
	private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

	/**
	 * Names of beans that are currently in creation. - 当前正在创建的bean的名称。
	 */
	private final ThreadLocal<Object> prototypesCurrentlyInCreation = new NamedThreadLocal<>("Prototype beans currently in creation");

	/**
	 * Application startup metrics.
	 **/
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	/**
	 * Create a new AbstractBeanFactory.
	 */
	public AbstractBeanFactory() {
	}

	/**
	 * Create a new AbstractBeanFactory with the given parent.
	 *
	 * @param parentBeanFactory parent bean factory, or {@code null} if none
	 * @see #getBean
	 */
	public AbstractBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}

	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		return doGetBean(name, null, null, false);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return doGetBean(name, requiredType, null, false);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		return doGetBean(name, null, args, false);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 *
	 * @param name the name of the bean to retrieve
	 * @param requiredType the required type of the bean to retrieve
	 * @param args arguments to use when creating a bean instance using explicit arguments
	 * (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 */
	public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object... args)
			throws BeansException {

		return doGetBean(name, requiredType, args, false);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 * -- 返回一个实例，该实例可以是指定bean的共享或独立的。
	 *
	 * @param name the name of the bean to retrieve
	 * @param requiredType the required type of the bean to retrieve -- 检索所需的bean类型
	 * @param args arguments to use when creating a bean instance using explicit arguments
	 * -- 使用显式参数创建Bean实例时要使用的参数
	 * (only applied when creating a new instance as opposed to retrieving an existing one - 仅在创建新实例而不是检索现有实例时适用)
	 * @param typeCheckOnly whether the instance is obtained for a type check, not for actual use -- 是否为实例检查而不是实际使用而获取实例
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 */
	@SuppressWarnings("unchecked")
	protected <T> T doGetBean(
			String name, // 名称,beanName 或者 alias 或者一个 &开头的name
			@Nullable Class<T> requiredType, //可以null，类型
			@Nullable Object[] args, //null
			boolean typeCheckOnly //false
	) throws BeansException {

		/**
		 * 转换beanName
		 *
		 * 1.别名：重定向出来真实 beanName
		 * 2.&开头的name：说明你要获取一个FactoryBean类型的bean
		 * 3.真实的BeanName
		 * FactoryBean：如果某个bean的配置非常复杂，使用Spring管理不易，不够灵活，想使用编码的方式去构建它，
		 * 那么你就可以提供一个构建该bean的工厂，这个工厂就是FactoryBean接口实现类，但是FactoryBean实例bean还是要使用Spring管理的，
		 * 这里就涉及两种对象，一种是FactoryBean接口实现类，一种是FactoryBean实现类内部管理的对象，
		 * 如果要拿FactoryBean接口实现类，使用getBean的时候传的name要使用 & 开头
		 * 如果要拿FactoryBean实现类内部管理的对象，直接传name即可，不需要 & 开头
		 *
		 * 	<bean class="com.javaxxl.bpp.Teacher" id="teacher"/> 最终该bd的beanName就是 teacher
		 */
		String beanName = transformedBeanName(name);

		//保留返回值
		Object beanInstance;

		/**
		 * Eagerly：热切地，急切的
		 * Eagerly check singleton cache for manually registered singletons.
		 *  -- 认真检查单例缓存是否有手动注册的单例
		 *  首先试图到一级缓存拿
		 *
		 *  一般首次创建的 bean 这里拿到的是null
		 *
		 *  到缓存中获取共享单实例
		 */
		Object sharedInstance = getSingleton(beanName);

		if (sharedInstance != null
				//TODO ？
				&& args == null) {
			//CASE1:缓存中有对象的实例，此时缓存数据可能是普通单实例，也可能是 FactoryBean 实例，所以吸引根据 name 来判断，并且返回数据

			if (logger.isTraceEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				} else {
					logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			/**
			 * 这里为什么又要套一层，为何不直接返回呢？
			 * 答：
			 * 其实，你从IOC容器中拿到的对象，它可能是普通单实例，也可能是 FactoryBean 实例。
			 * 如果是 FactoryBean 实例，这个时候还需要进行处理，主要是看 name 是否带 & 开头
			 * &开头，则说明要拿 FactoryBean 实例，否则为 FactoryBean 实例内部管理的bean
			 */
			beanInstance = getObjectForBeanInstance(sharedInstance,
					name,//用户传进来的name,可能是别名，可能待&
					beanName,//经过spring处理之后的beanName
					null);
		} else {
			//CASE2:缓存中没有数据，我们应该自己创建

			/**
			 *  @see AbstractBeanFactory#prototypesCurrentlyInCreation
			 *  例子：
			 *  假设两个原型模式对象 A 依赖 B，B 依赖 A
			 *  1.创建A的时候会向 {@link AbstractBeanFactory#prototypesCurrentlyInCreation} 中添加 A
			 *  2.创建A的早期对象
			 *  3.处理A的依赖，发现A依赖B
			 *  4.触发Spring.getBean(B.class)的操作
			 *  5.根据B的构造方法反射创建出了B的早期对象,并且向 {@link AbstractBeanFactory#prototypesCurrentlyInCreation} 中添加 B,此时有 A,B
			 *  6.spring处理B的依赖的时候发现了它依赖A
			 *  7.spring转头再次去获取A，触发getBean(A.class)
			 *  8.此时会执行到这里，因为此时 {@link AbstractBeanFactory#prototypesCurrentlyInCreation} 中已经有 A 了，所以 {@link AbstractBeanFactory#isPrototypeCurrentlyInCreation(java.lang.String)} 会返回 true
			 *  9.进入下面的if代码块，最终抛出了异常，成功阻止了原型模式的循环依赖
			 *
			 *  ThreadLocal<Object> prototypesCurrentlyInCreation 其实就是在一个ThreadLocal中存储
			 *
			 * @see AbstractBeanFactory#beforePrototypeCreation(java.lang.String) 在这个方法会塞入原型类型的beanName
			 */
			if (isPrototypeCurrentlyInCreation(beanName)) {

				/**
				 * Fail if we're already creating this bean instance:
				 * We're assumably within a circular reference.
				 * -- 如果我们已经在创建此bean实例，则失败：大概在循环引用中。
				 *
				 * 原型模式，循环依赖，直接抛出异常!
				 */
				throw new BeanCurrentlyInCreationException(beanName);
			}

			/** 父子容器 ignore start **************/
			// Check if bean definition exists in this factory.
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// Not found -> check parent.
				String nameToLookup = originalBeanName(name);
				if (parentBeanFactory instanceof AbstractBeanFactory) {
					return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
							nameToLookup, requiredType, args, typeCheckOnly);
				} else if (args != null) {
					// Delegation to parent with explicit args.
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				} else if (requiredType != null) {
					// No args -> delegate to standard getBean method.
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				} else {
					return (T) parentBeanFactory.getBean(nameToLookup);
				}
			}
			/** 父子容器 ignore end **************/

			if (!typeCheckOnly) {
				//首次创建的时候 typeCheckOnly 为 false，就会进来
				markBeanAsCreated(beanName);
			}

			StartupStep beanCreation = this.applicationStartup.start("spring.beans.instantiate").tag("beanName", name);

			try {
				if (requiredType != null) {
					beanCreation.tag("beanType", requiredType::toString);
				}

				/**
				 * 获取合并 bd 信息
				 * 为什么需要合并？因为bd支持继承
				 */
				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				//确定该 mbd 不是抽象类
				checkMergedBeanDefinition(mbd, beanName, args);

				/**
				 * Guarantee initialization of beans that the current bean depends on.
				 *  -- 确保当前bean依赖的bean的初始化。
				 *
				 *  <bean name = "A" depends-on = "B" .../>
				 *  <bean name = "B" .../>
				 *
				 *  说明，实例化A完成之前，B必须已经实例化了
				 *
				 *  但是有一个循环依赖问题:
				 *
				 *  <bean name = "A" depends-on = "B" .../>
				 *  <bean name = "B" depends-on = "A" .../>
				 *  这样的情况spring是无法处理的，需要报错！
				 *  spring是如何发现这样的错误配置，并且是如何实现的呢?
				 *  依靠2个map!!!
				 *  @see DefaultSingletonBeanRegistry#dependentBeanMap 记录依赖当前 beanName 的其他 beanName
				 *  @see DefaultSingletonBeanRegistry#dependenciesForBeanMap 记录当前 beanName 依赖的其他 beanName 集合
				 *
				 *  具体实现如下：
				 */
				String[] dependsOn = mbd.getDependsOn();
				if (dependsOn != null) {
					//depends-on = "a,b,c",并且当前beanName = "d"
					for (String dep : dependsOn) {
						//判断循环依赖
						if (isDependent(beanName, dep)) {
							//发生循环依赖，类似 <bean name = "A" depends-on = "B" .../>
							// <bean name = "B" depends-on = "A" .../>
							throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
						}

						/**
						 * 假设<bean name = "A" depends-on = "B" .../>
						 * 则 dep = B, beanName = A
						 */
						registerDependentBean(dep, beanName);
						try {
							//必须要实例化 当前 bean 所依赖的 实例。此处发生递归
							getBean(dep);
						} catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName, "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
						}
					}
				}

				/**
				 * 上面是准备好 bd
				 * 然后检查了 depends-on 的添加
				 * 并且看看 depends-on 没有发生循环依赖
				 *
				 * 下面就是根据 bd 创建 bean 实例的逻辑啦
				 */

				// Create bean instance.
				if (mbd.isSingleton()) {
					//单实例

					/**
					 * 该方法虽然也会看缓存
					 * 但是更倾向于创建实例并返回
					 * 大概率此时缓存中也是没有的
					 * 当然出了并发的情况
					 *
					 * 该方法还处理了循环依赖的问题
					 *
					 */
					sharedInstance = getSingleton(beanName, () -> {
						try {
							//真正实例化的地方
							return createBean(beanName, mbd, args);
						} catch (BeansException ex) {
							// Explicitly remove instance from singleton cache: It might have been put there
							// eagerly by the creation process, to allow for circular reference resolution.
							// Also remove any beans that received a temporary reference to the bean.
							destroySingleton(beanName);
							throw ex;
						}
					});

					/**
					 * 这里为什么又要套一层，为何不直接返回呢？
					 * 答：
					 * 其实，你从IOC容器中拿到的对象，它可能是普通单实例，也可能是 FactoryBean 实例。
					 * 如果是 FactoryBean 实例，这个时候还需要进行处理，主要是看 name 是否带 & 开头
					 * &开头，则说明要拿 FactoryBean 实例，否则为 FactoryBean 实例内部管理的bean
					 */
					beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}

				//原型
				else if (mbd.isPrototype()) {
					// It's a prototype 则 create a new instance.
					Object prototypeInstance = null;
					try {
						/**
						 * 在创建原型模式 bean 前执行的方法：
						 * 把 beanName 塞入当前线程的 ThreadLocal<Object> prototypesCurrentlyInCreation
						 * @see AbstractBeanFactory#prototypesCurrentlyInCreation
						 *
						 * 记录当前线程正在创建的原型对象的 beanName
						 */
						beforePrototypeCreation(beanName);

						//创建
						prototypeInstance = createBean(beanName, mbd, args);
					} finally {
						/**
						 * 创建原型前把 beanName 塞入，创建之后 remove
						 * @see AbstractBeanFactory#prototypesCurrentlyInCreation
						 */
						afterPrototypeCreation(beanName);
					}
					beanInstance = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				}

				//非单例，非原型
				else {
					String scopeName = mbd.getScope();
					if (!StringUtils.hasLength(scopeName)) {
						throw new IllegalStateException("No scope name defined for bean ´" + beanName + "'");
					}
					Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
					}
					try {
						Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							} finally {
								afterPrototypeCreation(beanName);
							}
						});
						beanInstance = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					} catch (IllegalStateException ex) {
						throw new ScopeNotActiveException(beanName, scopeName, ex);
					}
				}
			} catch (BeansException ex) {
				beanCreation.tag("exception", ex.getClass().toString());
				beanCreation.tag("message", String.valueOf(ex.getMessage()));
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			} finally {
				beanCreation.end();
			}
		}

		return adaptBeanInstance(name, beanInstance, requiredType);
	}

	@SuppressWarnings("unchecked")
	<T> T adaptBeanInstance(String name, Object bean, @Nullable Class<?> requiredType) {
		// Check if required type matches the type of the actual bean instance.
		if (requiredType != null && !requiredType.isInstance(bean)) {
			try {
				Object convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
				}
				return (T) convertedBean;
			} catch (TypeMismatchException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to convert bean '" + name + "' to required type '" +
							ClassUtils.getQualifiedName(requiredType) + "'", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}

	@Override
	public boolean containsBean(String name) {
		String beanName = transformedBeanName(name);
		if (containsSingleton(beanName) //  一级缓存中是否存在

				//beanName 是否有对应的 bd
				|| containsBeanDefinition(beanName)) {

			//如果一级缓存中有，或者有对应的 bd，则进来这里
			return (
					!BeanFactoryUtils.isFactoryDereference(name)
							|| isFactoryBean(name)
			);
		}

		//到此，一级缓存没有并且也没有beanName对应的 bd

		// Not found -> check parent.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			if (beanInstance instanceof FactoryBean) {
				return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
			} else {
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		}

		// No singleton instance found -> check bean definition.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isSingleton(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// In case of FactoryBean, return singleton status of created object if not a dereference.
		if (mbd.isSingleton()) {
			if (isFactoryBean(beanName, mbd)) {
				if (BeanFactoryUtils.isFactoryDereference(name)) {
					return true;
				}
				FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
				return factoryBean.isSingleton();
			} else {
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isPrototype(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isPrototype()) {
			// In case of FactoryBean, return singleton status of created object if not a dereference.
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
		}

		// Singleton or scoped - not a prototype.
		// However, FactoryBean may still produce a prototype object...
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			return false;
		}
		if (isFactoryBean(beanName, mbd)) {
			FactoryBean<?> fb = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged(
						(PrivilegedAction<Boolean>) () ->
								((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
										!fb.isSingleton()),
						getAccessControlContext());
			} else {
				return ((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
						!fb.isSingleton());
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		return isTypeMatch(name, typeToMatch, true);
	}

	/**
	 * Internal extended variant of {@link #isTypeMatch(String, ResolvableType)}
	 * to check whether the bean with the given name matches the specified type. Allow
	 * additional constraints to be applied to ensure that beans are not created early.
	 *
	 * @param name the name of the bean to query
	 * @param typeToMatch the type to match against (as a
	 * {@code ResolvableType})
	 * @return {@code true} if the bean type matches, {@code false} if it
	 * doesn't match or cannot be determined yet
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @see #getBean
	 * @see #getType
	 * @since 5.2
	 */
	protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		String beanName = transformedBeanName(name);
		boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);

		// Check manually registered singletons.
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
			if (beanInstance instanceof FactoryBean) {
				if (!isFactoryDereference) {
					Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
					return (type != null && typeToMatch.isAssignableFrom(type));
				} else {
					return typeToMatch.isInstance(beanInstance);
				}
			} else if (!isFactoryDereference) {
				if (typeToMatch.isInstance(beanInstance)) {
					// Direct match for exposed instance?
					return true;
				} else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
					// Generics potentially only match on the target class, not on the proxy...
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					Class<?> targetType = mbd.getTargetType();
					if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
						// Check raw class match as well, making sure it's exposed on the proxy.
						Class<?> classToMatch = typeToMatch.resolve();
						if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
							return false;
						}
						if (typeToMatch.isAssignableFrom(targetType)) {
							return true;
						}
					}
					ResolvableType resolvableType = mbd.targetType;
					if (resolvableType == null) {
						resolvableType = mbd.factoryMethodReturnType;
					}
					return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
				}
			}
			return false;
		} else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// null instance registered
			return false;
		}

		// No singleton instance found -> check bean definition.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
		}

		// Retrieve corresponding bean definition.
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();

		// Setup the types that we want to match against
		Class<?> classToMatch = typeToMatch.resolve();
		if (classToMatch == null) {
			classToMatch = FactoryBean.class;
		}
		Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
				new Class<?>[] { classToMatch } : new Class<?>[] { FactoryBean.class, classToMatch });

		// Attempt to predict the bean type
		Class<?> predictedType = null;

		// We're looking for a regular reference but we're a factory bean that has
		// a decorated bean definition. The target bean should be the same type
		// as FactoryBean would ultimately return.
		if (!isFactoryDereference && dbd != null && isFactoryBean(beanName, mbd)) {
			// We should only attempt if the user explicitly set lazy-init to true
			// and we know the merged bean definition is for a factory bean.
			if (!mbd.isLazyInit() || allowFactoryBeanInit) {
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				Class<?> targetType = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
				if (targetType != null && !FactoryBean.class.isAssignableFrom(targetType)) {
					predictedType = targetType;
				}
			}
		}

		// If we couldn't use the target type, try regular prediction.
		if (predictedType == null) {
			predictedType = predictBeanType(beanName, mbd, typesToMatch);
			if (predictedType == null) {
				return false;
			}
		}

		// Attempt to get the actual ResolvableType for the bean.
		ResolvableType beanType = null;

		// If it's a FactoryBean, we want to look at what it creates, not the factory class.
		if (FactoryBean.class.isAssignableFrom(predictedType)) {
			if (beanInstance == null && !isFactoryDereference) {
				beanType = getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit);
				predictedType = beanType.resolve();
				if (predictedType == null) {
					return false;
				}
			}
		} else if (isFactoryDereference) {
			// Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
			// type but we nevertheless are being asked to dereference a FactoryBean...
			// Let's check the original bean class and proceed with it if it is a FactoryBean.
			predictedType = predictBeanType(beanName, mbd, FactoryBean.class);
			if (predictedType == null || !FactoryBean.class.isAssignableFrom(predictedType)) {
				return false;
			}
		}

		// We don't have an exact type but if bean definition target type or the factory
		// method return type matches the predicted type then we can use that.
		if (beanType == null) {
			ResolvableType definedType = mbd.targetType;
			if (definedType == null) {
				definedType = mbd.factoryMethodReturnType;
			}
			if (definedType != null && definedType.resolve() == predictedType) {
				beanType = definedType;
			}
		}

		// If we have a bean type use it so that generics are considered
		if (beanType != null) {
			return typeToMatch.isAssignableFrom(beanType);
		}

		// If we don't have a bean type, fallback to the predicted type
		return typeToMatch.isAssignableFrom(predictedType);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getType(name, true);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);

		// Check manually registered singletons.
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
			if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
				return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
			} else {
				return beanInstance.getClass();
			}
		}

		// No singleton instance found -> check bean definition.
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// No bean definition found in this factory -> delegate to parent.
			return parentBeanFactory.getType(originalBeanName(name));
		}

		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// Check decorated bean definition, if any: We assume it'll be easier
		// to determine the decorated bean's type than the proxy's type.
		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
		if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
			RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
			Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
			if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
				return targetClass;
			}
		}

		Class<?> beanClass = predictBeanType(beanName, mbd);

		// Check bean class whether we're dealing with a FactoryBean.
		if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
			if (!BeanFactoryUtils.isFactoryDereference(name)) {
				// If it's a FactoryBean, we want to look at what it creates, not at the factory class.
				return getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit).resolve();
			} else {
				return beanClass;
			}
		} else {
			return (!BeanFactoryUtils.isFactoryDereference(name) ? beanClass : null);
		}
	}

	@Override
	public String[] getAliases(String name) {
		String beanName = transformedBeanName(name);
		List<String> aliases = new ArrayList<>();
		boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
		String fullBeanName = beanName;
		if (factoryPrefix) {
			fullBeanName = FACTORY_BEAN_PREFIX + beanName;
		}
		if (!fullBeanName.equals(name)) {
			aliases.add(fullBeanName);
		}
		String[] retrievedAliases = super.getAliases(beanName);
		String prefix = factoryPrefix ? FACTORY_BEAN_PREFIX : "";
		for (String retrievedAlias : retrievedAliases) {
			String alias = prefix + retrievedAlias;
			if (!alias.equals(name)) {
				aliases.add(alias);
			}
		}
		if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null) {
				aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
			}
		}
		return StringUtils.toStringArray(aliases);
	}

	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return this.parentBeanFactory;
	}

	@Override
	public boolean containsLocalBean(String name) {
		String beanName = transformedBeanName(name);
		return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) &&
				(!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
	}

	//---------------------------------------------------------------------
	// Implementation of ConfigurableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
			throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
		}
		if (this == parentBeanFactory) {
			throw new IllegalStateException("Cannot set parent bean factory to self");
		}
		this.parentBeanFactory = parentBeanFactory;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
	}

	@Override
	@Nullable
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setTempClassLoader(@Nullable ClassLoader tempClassLoader) {
		this.tempClassLoader = tempClassLoader;
	}

	@Override
	@Nullable
	public ClassLoader getTempClassLoader() {
		return this.tempClassLoader;
	}

	@Override
	public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
		this.cacheBeanMetadata = cacheBeanMetadata;
	}

	@Override
	public boolean isCacheBeanMetadata() {
		return this.cacheBeanMetadata;
	}

	@Override
	public void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver) {
		this.beanExpressionResolver = resolver;
	}

	@Override
	@Nullable
	public BeanExpressionResolver getBeanExpressionResolver() {
		return this.beanExpressionResolver;
	}

	@Override
	public void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
		Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
		this.propertyEditorRegistrars.add(registrar);
	}

	/**
	 * Return the set of PropertyEditorRegistrars.
	 */
	public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
		Assert.notNull(requiredType, "Required type must not be null");
		Assert.notNull(propertyEditorClass, "PropertyEditor class must not be null");
		this.customEditors.put(requiredType, propertyEditorClass);
	}

	@Override
	public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
		registerCustomEditors(registry);
	}

	/**
	 * Return the map of custom editors, with Classes as keys and PropertyEditor classes as values.
	 */
	public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
		return this.customEditors;
	}

	@Override
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	/**
	 * Return the custom TypeConverter to use, if any.
	 *
	 * @return the custom TypeConverter, or {@code null} if none specified
	 */
	@Nullable
	protected TypeConverter getCustomTypeConverter() {
		return this.typeConverter;
	}

	@Override
	public TypeConverter getTypeConverter() {
		TypeConverter customConverter = getCustomTypeConverter();
		if (customConverter != null) {
			return customConverter;
		} else {
			// Build default TypeConverter, registering custom editors.
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			typeConverter.setConversionService(getConversionService());
			registerCustomEditors(typeConverter);
			return typeConverter;
		}
	}

	@Override
	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		this.embeddedValueResolvers.add(valueResolver);
	}

	@Override
	public boolean hasEmbeddedValueResolver() {
		return !this.embeddedValueResolvers.isEmpty();
	}

	@Override
	@Nullable
	public String resolveEmbeddedValue(@Nullable String value) {
		if (value == null) {
			return null;
		}
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			result = resolver.resolveStringValue(result);
			if (result == null) {
				return null;
			}
		}
		return result;
	}

	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
		// Remove from old position, if any
		//保证不重复添加
		this.beanPostProcessors.remove(beanPostProcessor);
		// Add to end of list
		this.beanPostProcessors.add(beanPostProcessor);
	}

	/**
	 * Add new BeanPostProcessors that will get applied to beans created
	 * by this factory. To be invoked during factory configuration.
	 *
	 * @see #addBeanPostProcessor
	 * @since 5.3
	 */
	public void addBeanPostProcessors(Collection<? extends BeanPostProcessor> beanPostProcessors) {
		//保证不重复添加
		this.beanPostProcessors.removeAll(beanPostProcessors);
		this.beanPostProcessors.addAll(beanPostProcessors);
	}

	@Override
	public int getBeanPostProcessorCount() {
		return this.beanPostProcessors.size();
	}

	/**
	 * Return the list of BeanPostProcessors that will get applied
	 * to beans created with this factory.
	 */
	public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}

	/**
	 * Return the internal cache of pre-filtered post-processors,
	 * freshly (re-)building it if necessary.
	 *
	 * @since 5.3
	 */
	BeanPostProcessorCache getBeanPostProcessorCache() {
		BeanPostProcessorCache bpCache = this.beanPostProcessorCache;
		if (bpCache == null) {
			bpCache = new BeanPostProcessorCache();
			for (BeanPostProcessor bp : this.beanPostProcessors) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					bpCache.instantiationAware.add((InstantiationAwareBeanPostProcessor) bp);
					if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
						bpCache.smartInstantiationAware.add((SmartInstantiationAwareBeanPostProcessor) bp);
					}
				}
				if (bp instanceof DestructionAwareBeanPostProcessor) {
					bpCache.destructionAware.add((DestructionAwareBeanPostProcessor) bp);
				}
				if (bp instanceof MergedBeanDefinitionPostProcessor) {
					bpCache.mergedDefinition.add((MergedBeanDefinitionPostProcessor) bp);
				}
			}
			this.beanPostProcessorCache = bpCache;
		}
		return bpCache;
	}

	/**
	 * Return whether this factory holds a InstantiationAwareBeanPostProcessor
	 * that will get applied to singleton beans on creation.
	 *
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
	 */
	protected boolean hasInstantiationAwareBeanPostProcessors() {
		return !getBeanPostProcessorCache().instantiationAware.isEmpty();
	}

	/**
	 * Return whether this factory holds a DestructionAwareBeanPostProcessor
	 * that will get applied to singleton beans on shutdown.
	 *
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean hasDestructionAwareBeanPostProcessors() {
		return !getBeanPostProcessorCache().destructionAware.isEmpty();
	}

	@Override
	public void registerScope(String scopeName, Scope scope) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		Assert.notNull(scope, "Scope must not be null");
		if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
			throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
		}
		Scope previous = this.scopes.put(scopeName, scope);
		if (previous != null && previous != scope) {
			if (logger.isDebugEnabled()) {
				logger.debug("Replacing scope '" + scopeName + "' from [" + previous + "] to [" + scope + "]");
			}
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("Registering scope '" + scopeName + "' with implementation [" + scope + "]");
			}
		}
	}

	@Override
	public String[] getRegisteredScopeNames() {
		return StringUtils.toStringArray(this.scopes.keySet());
	}

	@Override
	@Nullable
	public Scope getRegisteredScope(String scopeName) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		return this.scopes.get(scopeName);
	}

	/**
	 * Set the security context provider for this bean factory. If a security manager
	 * is set, interaction with the user code will be executed using the privileged
	 * of the provided security context.
	 */
	public void setSecurityContextProvider(SecurityContextProvider securityProvider) {
		this.securityContextProvider = securityProvider;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "applicationStartup should not be null");
		this.applicationStartup = applicationStartup;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * Delegate the creation of the access control context to the
	 * {@link #setSecurityContextProvider SecurityContextProvider}.
	 */
	@Override
	public AccessControlContext getAccessControlContext() {
		return (this.securityContextProvider != null ?
				this.securityContextProvider.getAccessControlContext() :
				AccessController.getContext());
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		Assert.notNull(otherFactory, "BeanFactory must not be null");
		setBeanClassLoader(otherFactory.getBeanClassLoader());
		setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
		setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
		setConversionService(otherFactory.getConversionService());
		if (otherFactory instanceof AbstractBeanFactory) {
			AbstractBeanFactory otherAbstractFactory = (AbstractBeanFactory) otherFactory;
			this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
			this.customEditors.putAll(otherAbstractFactory.customEditors);
			this.typeConverter = otherAbstractFactory.typeConverter;
			this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
			this.scopes.putAll(otherAbstractFactory.scopes);
			this.securityContextProvider = otherAbstractFactory.securityContextProvider;
		} else {
			setTypeConverter(otherFactory.getTypeConverter());
			String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
			for (String scopeName : otherScopeNames) {
				this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
			}
		}
	}

	/**
	 * Return a 'merged' BeanDefinition for the given bean name,
	 * merging a child bean definition with its parent if necessary.
	 * <p>This {@code getMergedBeanDefinition} considers bean definition
	 * in ancestors as well.
	 *
	 * @param name the name of the bean to retrieve the merged definition for
	 * (may be an alias)
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	@Override
	public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
		String beanName = transformedBeanName(name);
		// Efficiently check whether bean definition exists in this factory.
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName);
		}
		// Resolve merged bean definition locally.
		return getMergedLocalBeanDefinition(beanName);
	}

	@Override
	public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null) {
			return (beanInstance instanceof FactoryBean);
		}
		// No singleton instance found -> check bean definition.
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			// No bean definition found in this factory -> delegate to parent.
			return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
		}
		return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
	}

	@Override
	public boolean isActuallyInCreation(String beanName) {
		return (isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName));
	}

	/**
	 * Return whether the specified prototype bean is currently in creation
	 * (within the current thread).
	 *
	 * @param beanName the name of the bean
	 */
	protected boolean isPrototypeCurrentlyInCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		return (curVal != null && //不为空

				//并且包含 beanName
				(curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
	}

	/**
	 * Callback before prototype creation. ： 创建原型之前进行回调 ，目的是为了避免原型模式的bean出现循环依赖
	 * <p>The default implementation register the prototype as currently in creation.
	 *
	 * @param beanName the name of the prototype about to be created
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void beforePrototypeCreation(String beanName) {
		//从 ThreadLocal<Object> 中拿到 curVal
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal == null) {
			//ThreadLocal<Object> 还是空的，则直接塞进去
			this.prototypesCurrentlyInCreation.set(beanName);
		}

		//ThreadLocal<Object> 中有值了，并且还是字符串，说明只有一个值
		else if (curVal instanceof String) {
			//把他们放入set中然后塞入 ThreadLocal<Object>

			Set<String> beanNameSet = new HashSet<>(2);
			beanNameSet.add((String) curVal);
			beanNameSet.add(beanName);
			this.prototypesCurrentlyInCreation.set(beanNameSet);
		}

		//ThreadLocal<Object> 中的值已经是 Set 了
		else {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.add(beanName);
		}
	}

	/**
	 * 创建原型前把 beanName 塞入，创建之后 remove
	 * Callback after prototype creation.
	 * <p>The default implementation marks the prototype as not in creation anymore.
	 *
	 * @param beanName the name of the prototype that has been created
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void afterPrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal instanceof String) {
			this.prototypesCurrentlyInCreation.remove();
		} else if (curVal instanceof Set) {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.remove(beanName);
			if (beanNameSet.isEmpty()) {
				this.prototypesCurrentlyInCreation.remove();
			}
		}
	}

	@Override
	public void destroyBean(String beanName, Object beanInstance) {
		destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
	}

	/**
	 * Destroy the given bean instance (usually a prototype instance
	 * obtained from this factory) according to the given bean definition.
	 *
	 * @param beanName the name of the bean definition
	 * @param bean the bean instance to destroy
	 * @param mbd the merged bean definition
	 */
	protected void destroyBean(String beanName, Object bean, RootBeanDefinition mbd) {
		new DisposableBeanAdapter(
				bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, getAccessControlContext()).destroy();
	}

	@Override
	public void destroyScopedBean(String beanName) {
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isSingleton() || mbd.isPrototype()) {
			throw new IllegalArgumentException(
					"Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
		}
		String scopeName = mbd.getScope();
		Scope scope = this.scopes.get(scopeName);
		if (scope == null) {
			throw new IllegalStateException("No Scope SPI registered for scope name '" + scopeName + "'");
		}
		Object bean = scope.remove(beanName);
		if (bean != null) {
			destroyBean(beanName, bean, mbd);
		}
	}

	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

	/**
	 * Return the bean name, stripping out the factory dereference prefix if necessary, and resolving aliases to canonical names.
	 * -- 返回Bean名称，必要时去除工厂取消引用前缀，并将别名解析为规范名称。
	 *
	 * 1.别名：重定向出来真实 beanName
	 * 2.&开头的name：说明你要获取一个FactoryBean类型的bean
	 * 3.真实的BeanName
	 *
	 *
	 * FactoryBean：如果某个bean的配置非常复杂，使用Spring管理不易，不够灵活，想使用编码的方式去构建它，
	 * 那么你就可以提供一个构建该bean的工厂，这个工厂就是FactoryBean接口实现类，但是FactoryBean实例bean还是要使用Spring管理的，
	 *
	 * 这里就涉及两种对象，一种是FactoryBean接口实现类，一种是FactoryBean实现类内部管理的对象，
	 * 如果要拿FactoryBean接口实现类，使用getBean的时候传的name要使用 & 开头
	 * 如果要拿FactoryBean实现类内部管理的对象，直接传name即可，不需要 & 开头
	 *
	 * @param name the user-specified name：名称,beanName 或者 alias 或者一个 &开头的name
	 * @return the transformed bean name
	 */
	protected String transformedBeanName(String name) {
		/**
		 * 假设别名aliasMap 中信息为：{"C":"B" , "B":"A"},则表示 A 有一个别名叫做 B,别名 B 有一个别名叫做 C
		 * 假设 getBean(C),则返回 A
		 */
		return canonicalName(
				/**
				 *  若 abc 则返回 abc;若 &abc 则返回 abc;若 &&abc 则返回 abc,
				 *  该方法返回的可能还是别名
				 */
				BeanFactoryUtils.transformedBeanName(name)
		);
	}

	/**
	 * Determine the original bean name, resolving locally defined aliases to canonical names.
	 *
	 * @param name the user-specified name
	 * @return the original bean name
	 */
	protected String originalBeanName(String name) {
		String beanName = transformedBeanName(name);
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			beanName = FACTORY_BEAN_PREFIX + beanName;
		}
		return beanName;
	}

	/**
	 * Initialize the given BeanWrapper with the custom editors registered with this factory.
	 * -- 使用在此工厂注册的自定义编辑器初始化给定的BeanWrapper。
	 *
	 * To be called for BeanWrappers that will create
	 * and populate bean instances.
	 * <p>The default implementation delegates to {@link #registerCustomEditors}.
	 * Can be overridden in subclasses.
	 *
	 * @param bw the BeanWrapper to initialize
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		/**
		 * 设置类型转换器
		 * @see GenericConversionService
		 */
		bw.setConversionService(getConversionService());
		//注册自定义的属性编辑器
		registerCustomEditors(bw);
	}

	/**
	 * Initialize the given PropertyEditorRegistry with the custom editors
	 * that have been registered with this BeanFactory.
	 * <p>To be called for BeanWrappers that will create and populate bean
	 * instances, and for SimpleTypeConverter used for constructor argument
	 * and factory method type conversion.
	 *
	 * @param registry the PropertyEditorRegistry to initialize
	 */
	protected void registerCustomEditors(PropertyEditorRegistry registry) {
		if (registry instanceof PropertyEditorRegistrySupport) {
			((PropertyEditorRegistrySupport) registry).useConfigValueEditors();
		}
		if (!this.propertyEditorRegistrars.isEmpty()) {
			for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
				try {
					registrar.registerCustomEditors(registry);
				} catch (BeanCreationException ex) {
					Throwable rootCause = ex.getMostSpecificCause();
					if (rootCause instanceof BeanCurrentlyInCreationException) {
						BeanCreationException bce = (BeanCreationException) rootCause;
						String bceBeanName = bce.getBeanName();
						if (bceBeanName != null && isCurrentlyInCreation(bceBeanName)) {
							if (logger.isDebugEnabled()) {
								logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
										"] failed because it tried to obtain currently created bean '" +
										ex.getBeanName() + "': " + ex.getMessage());
							}
							onSuppressedException(ex);
							continue;
						}
					}
					throw ex;
				}
			}
		}
		if (!this.customEditors.isEmpty()) {
			this.customEditors.forEach((requiredType, editorClass) ->
					registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass)));
		}
	}

	/**
	 * Return a merged RootBeanDefinition, traversing the parent bean definition
	 * if the specified bean corresponds to a child bean definition.
	 *
	 * @param beanName the name of the bean to retrieve the merged definition for
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
		// Quick check on the concurrent map first, with minimal locking.
		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
		if (mbd != null //存在
				//并且还没有过期

				&& !mbd.stale) {
			return mbd;
		}

		return getMergedBeanDefinition(

				beanName,

				/**
				 * 到map中拿
				 */
				getBeanDefinition(beanName)
		);
	}

	/**
	 * Return a RootBeanDefinition for the given top-level bean, by merging with
	 * the parent if the given bean's definition is a child bean definition.
	 *
	 * @param beanName the name of the bean definition
	 * @param bd the original bean definition (Root/ChildBeanDefinition)
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd) throws BeanDefinitionStoreException {
		return getMergedBeanDefinition(beanName, bd, null);
	}

	/**
	 * 为什么需要合并？因为bd支持继承
	 *
	 * Return a RootBeanDefinition for the given bean, by merging with the parent if the given bean's definition is a child bean definition.
	 * -- 如果给定bean的定义是子bean定义，则通过与父级合并返回RootBeanDefinition。
	 *
	 * @param beanName the name of the bean definition
	 * @param bd the original bean definition (Root/ChildBeanDefinition)
	 * @param containingBd the containing bean definition in case of inner bean, or {@code null} in case of a top-level bean
	 * @return a (potentially merged) RootBeanDefinition for the given bean
	 * @throws BeanDefinitionStoreException in case of an invalid bean definition
	 */
	protected RootBeanDefinition getMergedBeanDefinition(
			String beanName,
			BeanDefinition bd,
			@Nullable BeanDefinition containingBd)
			throws BeanDefinitionStoreException {

		synchronized (this.mergedBeanDefinitions) {

			//合并后的bd 信息
			RootBeanDefinition mbd = null;

			//当前beanName对应的过期的mbd信息
			RootBeanDefinition previous = null;

			// Check with full lock now in order to enforce the same merged instance.：立即检查完全锁定，以强制执行相同的合并实例。
			if (containingBd == null) {
				mbd = this.mergedBeanDefinitions.get(beanName);
			}

			if (mbd == null //还没有合并

					//mbd 已经过期了
					|| mbd.stale) {

				//把旧的 mbd 父子给 previous
				previous = mbd;

				if (bd.getParentName() == null) {
					//当前 beanName 对应的 bd 根本就没有 parent 属性

					// Use copy of given root bean definition.
					if (bd instanceof RootBeanDefinition) {
						mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
					} else {
						mbd = new RootBeanDefinition(bd);
					}
				} else {
					//当前 beanName 对应的 bd 有 parent 属性

					// Child bean definition: needs to be merged with parent.

					//parent 对应的 bd
					BeanDefinition pbd;
					try {
						String parentBeanName = transformedBeanName(bd.getParentName());
						if (!beanName.equals(parentBeanName)) {
							//如果子 beanName 不等于父 parentBeanName ,此情况为普通情况

							/**
							 * 递归调用，最终返回父 bd 信息
							 */
							pbd = getMergedBeanDefinition(parentBeanName);
						}

						//如果子 beanName 等于父 parentBeanName ,此情况为特殊情况
						else {
							BeanFactory parent = getParentBeanFactory();
							if (parent instanceof ConfigurableBeanFactory) {
								pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
							} else {
								throw new NoSuchBeanDefinitionException(parentBeanName, "Parent name '" + parentBeanName + "' is equal to bean name '" + beanName + "': cannot be resolved without a ConfigurableBeanFactory parent");
							}
						}
					} catch (NoSuchBeanDefinitionException ex) {
						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName, "Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
					}
					// Deep copy with overridden values.:深度拷贝
					mbd = new RootBeanDefinition(pbd);
					mbd.overrideFrom(bd);
				}

				// Set default singleton scope, if not configured before.
				if (!StringUtils.hasLength(mbd.getScope())) {
					mbd.setScope(SCOPE_SINGLETON);
				}

				// A bean contained in a non-singleton bean cannot be a singleton itself.
				// Let's correct this on the fly here, since this might be the result of
				// parent-child merging for the outer bean, in which case the original inner bean
				// definition will not have inherited the merged outer bean's singleton status.
				if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
					mbd.setScope(containingBd.getScope());
				}

				// Cache the merged bean definition for the time being
				// (it might still get re-merged later on in order to pick up metadata changes)
				if (containingBd == null

						//true
						&& isCacheBeanMetadata()) {

					//缓存合并后的mbd。提供性能
					this.mergedBeanDefinitions.put(beanName, mbd);
				}
			}
			if (previous != null) {
				copyRelevantMergedBeanDefinitionCaches(previous, mbd);
			}
			return mbd;
		}
	}

	private void copyRelevantMergedBeanDefinitionCaches(RootBeanDefinition previous, RootBeanDefinition mbd) {
		if (ObjectUtils.nullSafeEquals(mbd.getBeanClassName(), previous.getBeanClassName()) &&
				ObjectUtils.nullSafeEquals(mbd.getFactoryBeanName(), previous.getFactoryBeanName()) &&
				ObjectUtils.nullSafeEquals(mbd.getFactoryMethodName(), previous.getFactoryMethodName())) {
			ResolvableType targetType = mbd.targetType;
			ResolvableType previousTargetType = previous.targetType;
			if (targetType == null || targetType.equals(previousTargetType)) {
				mbd.targetType = previousTargetType;
				mbd.isFactoryBean = previous.isFactoryBean;
				mbd.resolvedTargetType = previous.resolvedTargetType;
				mbd.factoryMethodReturnType = previous.factoryMethodReturnType;
				mbd.factoryMethodToIntrospect = previous.factoryMethodToIntrospect;
			}
		}
	}

	/**
	 * Check the given merged bean definition,
	 * potentially throwing validation exceptions.
	 *
	 * @param mbd the merged bean definition to check
	 * @param beanName the name of the bean
	 * @param args the arguments for bean creation, if any
	 * @throws BeanDefinitionStoreException in case of validation failure
	 */
	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args) throws BeanDefinitionStoreException {
		if (mbd.isAbstract()) {
			throw new BeanIsAbstractException(beanName);
		}
	}

	/**
	 * Remove the merged bean definition for the specified bean,recreating it on next access.
	 * -- 删除指定bean的合并bean定义，在下次访问时重新创建。
	 *
	 * @param beanName the bean name to clear the merged definition for
	 */
	protected void clearMergedBeanDefinition(String beanName) {

		//如果合并后的bd信息缓存中，有当前 beanName 对象的 bd 信息，需要把当前 mbd 信息设置为过期数据
		RootBeanDefinition bd = this.mergedBeanDefinitions.get(beanName);
		if (bd != null) {
			bd.stale = true;
		}
	}

	/**
	 * Clear the merged bean definition cache, removing entries for beans
	 * which are not considered eligible for full metadata caching yet.
	 * <p>Typically triggered after changes to the original bean definitions,
	 * e.g. after applying a {@code BeanFactoryPostProcessor}. Note that metadata
	 * for beans which have already been created at this point will be kept around.
	 *
	 * @since 4.2
	 */
	public void clearMetadataCache() {
		this.mergedBeanDefinitions.forEach((beanName, bd) -> {
			if (!isBeanEligibleForMetadataCaching(beanName)) {
				bd.stale = true;
			}
		});
	}

	/**
	 * Resolve the bean class for the specified bean definition,
	 * resolving a bean class name into a Class reference (if necessary)
	 * and storing the resolved Class in the bean definition for further use.
	 *
	 * @param mbd the merged bean definition to determine the class for
	 * @param beanName the name of the bean (for error handling purposes)
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 * (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the resolved bean class (or {@code null} if none)
	 * @throws CannotLoadBeanClassException if we failed to load the class
	 */
	@Nullable
	protected Class<?> resolveBeanClass(RootBeanDefinition mbd, String beanName, Class<?>... typesToMatch) throws CannotLoadBeanClassException {
		try {
			if (mbd.hasBeanClass()) {
				//如果有直接返回
				return mbd.getBeanClass();
			}

			/**
			 * @see AbstractBeanDefinition#beanClass 该参数为null的时候 会往下执行
			 */

			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedExceptionAction<Class<?>>) () ->

						//通过该方法
						doResolveBeanClass(mbd, typesToMatch), getAccessControlContext());
			} else {
				//如果没有则解析并加载
				return doResolveBeanClass(mbd, typesToMatch);
			}
		} catch (PrivilegedActionException pae) {
			ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (LinkageError err) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
		}
	}

	/**
	 * @param mbd bd
	 * @param typesToMatch 可能为空
	 */
	@Nullable
	private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch) throws ClassNotFoundException {

		/**
		 * @see AbstractBeanFactory#beanClassLoader
		 */
		ClassLoader beanClassLoader = getBeanClassLoader();
		ClassLoader dynamicLoader = beanClassLoader;
		boolean freshResolve = false;

		if (!ObjectUtils.isEmpty(typesToMatch)) {
			//typesToMatch 不为空

			// When just doing type checks (i.e. not creating an actual instance yet),
			// use the specified temporary class loader (e.g. in a weaving scenario).
			ClassLoader tempClassLoader = getTempClassLoader();
			if (tempClassLoader != null) {
				dynamicLoader = tempClassLoader;
				freshResolve = true;
				if (tempClassLoader instanceof DecoratingClassLoader) {
					DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
					for (Class<?> typeToMatch : typesToMatch) {
						dcl.excludeClass(typeToMatch.getName());
					}
				}
			}
		}

		/**
		 * bean中的 class 属性
		 */
		String className = mbd.getBeanClassName();
		if (className != null) {
			//<bean  class = "xxxxx" /> 说明配置了 class

			Object evaluated = evaluateBeanDefinitionString(className, mbd);
			if (!className.equals(evaluated)) {
				// A dynamically resolved expression, supported as of 4.2...
				if (evaluated instanceof Class) {
					return (Class<?>) evaluated;
				} else if (evaluated instanceof String) {
					className = (String) evaluated;
					freshResolve = true;
				} else {
					throw new IllegalStateException("Invalid class name expression result: " + evaluated);
				}
			}
			if (freshResolve) {
				// When resolving against a temporary class loader, exit early in order
				// to avoid storing the resolved Class in the bean definition.
				if (dynamicLoader != null) {
					try {
						return dynamicLoader.loadClass(className);
					} catch (ClassNotFoundException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("Could not load class [" + className + "] from " + dynamicLoader + ": " + ex);
						}
					}
				}

				//加载到jvm中
				return ClassUtils.forName(className, dynamicLoader);
			}
		}

		// Resolve regularly, caching the result in the BeanDefinition... -- 定期解析，将结果缓存在BeanDefinition中...
		return mbd.resolveBeanClass(beanClassLoader);
	}

	/**
	 * Evaluate the given String as contained in a bean definition,
	 * potentially resolving it as an expression.
	 *
	 * @param value the value to check
	 * @param beanDefinition the bean definition that the value comes from
	 * @return the resolved value
	 * @see #setBeanExpressionResolver
	 */
	@Nullable
	protected Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
		if (this.beanExpressionResolver == null) {
			return value;
		}

		Scope scope = null;
		if (beanDefinition != null) {
			String scopeName = beanDefinition.getScope();
			if (scopeName != null) {
				scope = getRegisteredScope(scopeName);
			}
		}
		return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
	}

	/**
	 * Predict the eventual bean type (of the processed bean instance) for the
	 * specified bean. Called by {@link #getType} and {@link #isTypeMatch}.
	 * Does not need to handle FactoryBeans specifically, since it is only
	 * supposed to operate on the raw bean type.
	 * <p>This implementation is simplistic in that it is not able to
	 * handle factory methods and InstantiationAwareBeanPostProcessors.
	 * It only predicts the bean type correctly for a standard bean.
	 * To be overridden in subclasses, applying more sophisticated type detection.
	 *
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition to determine the type for
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 * (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the type of the bean, or {@code null} if not predictable
	 */
	@Nullable
	protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		Class<?> targetType = mbd.getTargetType();
		if (targetType != null) {
			return targetType;
		}
		if (mbd.getFactoryMethodName() != null) {
			return null;
		}
		return resolveBeanClass(mbd, beanName, typesToMatch);
	}

	/**
	 * Check whether the given bean is defined as a {@link FactoryBean}.
	 *
	 * @param beanName the name of the bean
	 * @param mbd the corresponding bean definition
	 */
	protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
		Boolean result = mbd.isFactoryBean;
		if (result == null) {
			Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
			result = (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
			mbd.isFactoryBean = result;
		}
		return result;
	}

	/**
	 * Determine the bean type for the given FactoryBean definition, as far as possible.
	 * Only called if there is no singleton instance registered for the target bean
	 * already. The implementation is allowed to instantiate the target factory bean if
	 * {@code allowInit} is {@code true} and the type cannot be determined another way;
	 * otherwise it is restricted to introspecting signatures and related metadata.
	 * <p>If no {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE} if set on the bean definition
	 * and {@code allowInit} is {@code true}, the default implementation will create
	 * the FactoryBean via {@code getBean} to call its {@code getObjectType} method.
	 * Subclasses are encouraged to optimize this, typically by inspecting the generic
	 * signature of the factory bean class or the factory method that creates it.
	 * If subclasses do instantiate the FactoryBean, they should consider trying the
	 * {@code getObjectType} method without fully populating the bean. If this fails,
	 * a full FactoryBean creation as performed by this implementation should be used
	 * as fallback.
	 *
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @param allowInit if initialization of the FactoryBean is permitted if the type
	 * cannot be determined another way
	 * @return the type for the bean if determinable, otherwise {@code ResolvableType.NONE}
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 * @since 5.2
	 */
	protected ResolvableType getTypeForFactoryBean(String beanName, RootBeanDefinition mbd, boolean allowInit) {
		ResolvableType result = getTypeForFactoryBeanFromAttributes(mbd);
		if (result != ResolvableType.NONE) {
			return result;
		}

		if (allowInit && mbd.isSingleton()) {
			try {
				FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
				Class<?> objectType = getTypeForFactoryBean(factoryBean);
				return (objectType != null ? ResolvableType.forClass(objectType) : ResolvableType.NONE);
			} catch (BeanCreationException ex) {
				if (ex.contains(BeanCurrentlyInCreationException.class)) {
					logger.trace(LogMessage.format("Bean currently in creation on FactoryBean type check: %s", ex));
				} else if (mbd.isLazyInit()) {
					logger.trace(LogMessage.format("Bean creation exception on lazy FactoryBean type check: %s", ex));
				} else {
					logger.debug(LogMessage.format("Bean creation exception on eager FactoryBean type check: %s", ex));
				}
				onSuppressedException(ex);
			}
		}
		return ResolvableType.NONE;
	}

	/**
	 * Determine the bean type for a FactoryBean by inspecting its attributes for a
	 * {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE} value.
	 *
	 * @param attributes the attributes to inspect
	 * @return a {@link ResolvableType} extracted from the attributes or
	 * {@code ResolvableType.NONE}
	 * @since 5.2
	 */
	ResolvableType getTypeForFactoryBeanFromAttributes(AttributeAccessor attributes) {
		Object attribute = attributes.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
		if (attribute instanceof ResolvableType) {
			return (ResolvableType) attribute;
		}
		if (attribute instanceof Class) {
			return ResolvableType.forClass((Class<?>) attribute);
		}
		return ResolvableType.NONE;
	}

	/**
	 * Determine the bean type for the given FactoryBean definition, as far as possible.
	 * Only called if there is no singleton instance registered for the target bean already.
	 * <p>The default implementation creates the FactoryBean via {@code getBean}
	 * to call its {@code getObjectType} method. Subclasses are encouraged to optimize
	 * this, typically by just instantiating the FactoryBean but not populating it yet,
	 * trying whether its {@code getObjectType} method already returns a type.
	 * If no type found, a full FactoryBean creation as performed by this implementation
	 * should be used as fallback.
	 *
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @return the type for the bean if determinable, or {@code null} otherwise
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 * @deprecated since 5.2 in favor of {@link #getTypeForFactoryBean(String, RootBeanDefinition, boolean)}
	 */
	@Nullable
	@Deprecated
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		return getTypeForFactoryBean(beanName, mbd, true).resolve();
	}

	/**
	 * Mark the specified bean as already created (or about to be created).-- 将指定的bean标记为已经创建（或将要创建）。
	 * <p>This allows the bean factory to optimize its caching for repeated creation of the specified bean. -- 这允许bean工厂优化其缓存以重复创建指定的bean。
	 *
	 * @param beanName the name of the bean
	 */
	protected void markBeanAsCreated(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			//Set<String> alreadyCreated 其实就是一个Set

			synchronized (this.mergedBeanDefinitions) {
				if (!this.alreadyCreated.contains(beanName)) {
					// Let the bean definition get re-merged now that we're actually creating
					// the bean... just in case some of its metadata changed in the meantime.
					clearMergedBeanDefinition(beanName);
					this.alreadyCreated.add(beanName);
				}
			}
		}
	}

	/**
	 * Perform appropriate cleanup of cached metadata after bean creation failed.
	 *
	 * @param beanName the name of the bean
	 */
	protected void cleanupAfterBeanCreationFailure(String beanName) {
		synchronized (this.mergedBeanDefinitions) {
			this.alreadyCreated.remove(beanName);
		}
	}

	/**
	 * Determine whether the specified bean is eligible for having
	 * its bean definition metadata cached.
	 *
	 * @param beanName the name of the bean
	 * @return {@code true} if the bean's metadata may be cached
	 * at this point already
	 */
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return this.alreadyCreated.contains(beanName);
	}

	/**
	 * Remove the singleton instance (if any) for the given bean name,
	 * but only if it hasn't been used for other purposes than type checking.
	 *
	 * @param beanName the name of the bean
	 * @return {@code true} if actually removed, {@code false} otherwise
	 */
	protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			//如果进来：说明当前 bean 尚未创建完成

			removeSingleton(beanName);
			return true;
		} else {
			//如果进来：说明当前 bean 已经创建完成
			return false;
		}
	}

	/**
	 * Check whether this factory's bean creation phase already started,
	 * i.e. whether any bean has been marked as created in the meantime.
	 *
	 * @see #markBeanAsCreated
	 * @since 4.2.2
	 */
	protected boolean hasBeanCreationStarted() {
		return !this.alreadyCreated.isEmpty();
	}

	/**
	 * Get the object for the given bean instance,
	 *
	 * either the bean instance itself or its created object in case of a FactoryBean.
	 * -- 要么是该bean自身或者是FactoryBean创建的bean
	 *
	 * @param beanInstance the shared bean instance
	 * @param name the name that may include factory dereference prefix
	 * @param beanName the canonical bean name
	 * @param mbd the merged bean definition
	 * @return the object to expose for the bean
	 */
	protected Object getObjectForBeanInstance(Object beanInstance,
			/**缓存中拿到的实例 {@link DefaultSingletonBeanRegistry#getSingleton(java.lang.String)}, 其实，你从IOC容器中拿到的对象，
			 * 它可能是普通单实例，也可能是 FactoryBean 实例。如果是 FactoryBean 实例，这个时候还需要进行处理，
			 * 主要是看 name 是否带 & 开头,&开头，则说明要拿 FactoryBean 实例，否则为 FactoryBean 实例内部管理的bean*/
			String name,  //用户传入的未处理&的name
			String beanName, /** 处理 & 以及 alias 之后 的 {@link AbstractBeanFactory#transformedBeanName(java.lang.String)}  ***/
			@Nullable RootBeanDefinition mbd) { //合并过后的bd

		// Don't let calling code try to dereference the factory if the bean isn't a factory.-- 如果Bean不是工厂，则不要让调用代码尝试取消引用工厂。
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			//说明name 是 & 开头的
			if (beanInstance instanceof NullBean) {
				// null
				return beanInstance;
			}
			if (!(beanInstance instanceof FactoryBean)) {
				//当前的 beanInstance 实例 不是 FactoryBean 实现，
				//
				// 则抛出异常，因为你想要的就是一个 FactoryBean 实现，但是IOC中并没有
				throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
			}
			/**
			 * 到这里的前提
			 * 1.beanInstance 不是 null 类型
			 * 2.beanInstance 是一个 FactoryBean 类型
			 */
			if (mbd != null) {
				mbd.isFactoryBean = true;
			}
			return beanInstance;
		}

		/**
		 * 到这里的前提:
		 * 1.name 并没有 & 开头
		 *
		 * 则
		 * 1.当前bean实例就是普通单实例
		 * 2.当前bean实例是FactoryBean实例，但是本次请求要拿的是FactoryBean实现内部管理的实例
		 */

		// Now we have the bean instance, which may be a normal bean or a FactoryBean.-- 现在我们有了bean实例，它可以是普通bean或FactoryBean。
		// If it's a FactoryBean, we use it to create a bean instance, unless the caller actually wants a reference to the factory.
		// -- 如果它是FactoryBean，则除非调用者实际上想要引用该工厂，否则我们将使用它创建一个bean实例。
		if (!(beanInstance instanceof FactoryBean)) {
			//如果不是 FactoryBean 类型，则返回当前实例,其实是一个普通对象单实例
			return beanInstance;
		}

		/**
		 * 到这里的前提:
		 * 1.name 并没有 & 开头
		 * 2.当前 beanInstance 是一个 FactoryBean 实例
		 */

		//保存 FactoryBean#getObject()方法返回值的引用
		Object object = null;
		if (mbd != null) {
			mbd.isFactoryBean = true;
		} else {
			/**
			 * 尝试到缓存中获取 {@link FactoryBean#getObject()} 返回值
			 * @see FactoryBeanRegistrySupport#factoryBeanObjectCache
			 */
			object = getCachedObjectForFactoryBean(beanName);
		}
		//如果缓存中没有，则去创建！！！

		/**
		 * @see org.springframework.aop.framework.ProxyFactoryBean#getObject() 典型使用案例！！！
		 */

		if (object == null) {
			/**
			 * 到这里说明，当前实例还没有从 {@link FactoryBean#getObject()} 获取过,缓存中没有
			 * 下面需要 {@link FactoryBean#getObject()} 创建
			 */
			// Return bean instance from factory.
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// Caches object obtained from FactoryBean if it is a singleton.:缓存从FactoryBean获得的对象（如果是单例）。

			if (mbd == null // 大部分调用该方法的地方都会传 null

					//判断spring中是否有当前 beanName 对应的 BD 信息
					&& containsBeanDefinition(beanName)) {

				/**
				 * 获取合并后的 bd
				 * 为什么需要合并？
				 * 答：因为我们的 bean 标签是支持继承的(parent属性)
				 */
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			//synthetic:合成的,默认值为 false，表示这是一个用户对象，如果为true，则表示是系统对象
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			/**
			 * 从 {@link FactoryBean#getObject()} 获取实例
			 */
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}

	/**
	 * Determine whether the given bean name is already in use within this factory,
	 * i.e. whether there is a local bean or alias registered under this name or
	 * an inner bean created with this name.
	 *
	 * @param beanName the name to check
	 */
	public boolean isBeanNameInUse(String beanName) {
		return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
	}

	/**
	 * Determine whether the given bean requires destruction on shutdown.
	 * <p>The default implementation checks the DisposableBean interface as well as
	 * a specified destroy method and registered DestructionAwareBeanPostProcessors.
	 *
	 * @param bean the bean instance to check
	 * @param mbd the corresponding bean definition
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see AbstractBeanDefinition#getDestroyMethodName()
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
		return (
				bean.getClass() != NullBean.class //如果 bean 是 null，肯定返回 false
						&&

						//前提：bean 不是 null
						(
								/**
								 * 是否指定了 destory-method 方法
								 * 或者 是否是 {@link DisposableBean} 或者{@link AutoCloseable} 的实现
								 */
								DisposableBeanAdapter.hasDestroyMethod(bean, mbd)
										||
										(
												/**
												 * 是否有{@link DestructionAwareBeanPostProcessor}类型的后处理器
												 */
												hasDestructionAwareBeanPostProcessors()

														&&

														/**
														 * 通过后处理器框架决定是否进行 析构 回调
														 * @see DestructionAwareBeanPostProcessor
														 */
														DisposableBeanAdapter.hasApplicableProcessors(bean, getBeanPostProcessorCache().destructionAware)

										)
						)
		);
	}

	/**
	 * Add the given bean to the list of disposable beans in this factory,
	 * registering its DisposableBean interface and/or the given destroy method
	 * to be called on factory shutdown (if applicable). Only applies to singletons.
	 *
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 * @param mbd the bean definition for the bean
	 * @see RootBeanDefinition#isSingleton
	 * @see RootBeanDefinition#getDependsOn
	 * @see #registerDisposableBean
	 * @see #registerDependentBean
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
		if (!mbd.isPrototype() //原型模式肯定是不会注册的

				&& requiresDestruction(bean, mbd)) {

			if (mbd.isSingleton()) {
				// Register a DisposableBean implementation that performs all destruction
				// work for the given bean: DestructionAwareBeanPostProcessors,
				// DisposableBean interface, custom destroy method.
				// 注册一个DisposableBean实现，该实现将执行给定bean的所有销毁工作：DestructionAwareBeanPostProcessors，DisposableBean接口，自定义destroy方法。

				/**
				 * 给当前单实例注册回调适配器，适配器内根据当前bean
				 * 实例是继承接口还是通过自定义方法来决定调用拿个方法完成析构
				 *
				 * @see DisposableBeanAdapter#destroy()
				 */
				registerDisposableBean(
						beanName,
						/**
						 * 适配器
						 */
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, acc)
				);
			} else {
				// A bean with a custom scope...
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
				}
				scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(
						bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, acc));
			}
		}
	}

	//---------------------------------------------------------------------
	// Abstract methods to be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Check if this bean factory contains a bean definition with the given name.
	 * Does not consider any hierarchy this factory may participate in.
	 * Invoked by {@code containsBean} when no cached singleton instance is found.
	 * <p>Depending on the nature of the concrete bean factory implementation,
	 * this operation might be expensive (for example, because of directory lookups
	 * in external registries). However, for listable bean factories, this usually
	 * just amounts to a local hash lookup: The operation is therefore part of the
	 * public interface there. The same implementation can serve for both this
	 * template method and the public interface method in that case.
	 *
	 * @param beanName the name of the bean to look for
	 * @return if this bean factory contains a bean definition with the given name
	 * @see #containsBean
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 */
	protected abstract boolean containsBeanDefinition(String beanName);

	/**
	 * Return the bean definition for the given bean name.
	 * Subclasses should normally implement caching, as this method is invoked
	 * by this class every time bean definition metadata is needed.
	 * <p>Depending on the nature of the concrete bean factory implementation,
	 * this operation might be expensive (for example, because of directory lookups
	 * in external registries). However, for listable bean factories, this usually
	 * just amounts to a local hash lookup: The operation is therefore part of the
	 * public interface there. The same implementation can serve for both this
	 * template method and the public interface method in that case.
	 *
	 * @param beanName the name of the bean to find a definition for
	 * @return the BeanDefinition for this prototype name (never {@code null})
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if the bean definition cannot be resolved
	 * @throws BeansException in case of errors
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#getBeanDefinition
	 */
	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	/**
	 * Create a bean instance for the given merged bean definition (and arguments).：为给定的 mbd（和参数）创建一个bean实例。
	 * The bean definition will already have been merged with the parent definition in case of a child definition.：如果当前bean是一个子bd，则已经跟它的父bd合并过了
	 * <p>All bean retrieval methods delegate to this method for actual bean creation.:所有bean检索方法都委托该方法进行实际的bean创建。!!!!!!!!!
	 *
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @param args explicit arguments to use for constructor or factory method invocation
	 * @return a new instance of the bean
	 * @throws BeanCreationException if the bean could not be created
	 */
	protected abstract Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) throws BeanCreationException;

	/**
	 * CopyOnWriteArrayList which resets the beanPostProcessorCache field on modification.
	 *
	 * @since 5.3
	 */
	private class BeanPostProcessorCacheAwareList extends CopyOnWriteArrayList<BeanPostProcessor> {

		@Override
		public BeanPostProcessor set(int index, BeanPostProcessor element) {
			BeanPostProcessor result = super.set(index, element);
			beanPostProcessorCache = null;
			return result;
		}

		@Override
		public boolean add(BeanPostProcessor o) {
			boolean success = super.add(o);
			beanPostProcessorCache = null;
			return success;
		}

		@Override
		public void add(int index, BeanPostProcessor element) {
			super.add(index, element);
			beanPostProcessorCache = null;
		}

		@Override
		public BeanPostProcessor remove(int index) {
			BeanPostProcessor result = super.remove(index);
			beanPostProcessorCache = null;
			return result;
		}

		@Override
		public boolean remove(Object o) {
			boolean success = super.remove(o);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean success = super.removeAll(c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean success = super.retainAll(c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean addAll(Collection<? extends BeanPostProcessor> c) {
			boolean success = super.addAll(c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean addAll(int index, Collection<? extends BeanPostProcessor> c) {
			boolean success = super.addAll(index, c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean removeIf(Predicate<? super BeanPostProcessor> filter) {
			boolean success = super.removeIf(filter);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public void replaceAll(UnaryOperator<BeanPostProcessor> operator) {
			super.replaceAll(operator);
			beanPostProcessorCache = null;
		}
	}

	/**
	 * Internal cache of pre-filtered post-processors.
	 *
	 * 后处理器缓存
	 *
	 * @since 5.3
	 */
	static class BeanPostProcessorCache {

		/**
		 * Instantiation 阶段执行的后处理器
		 */
		final List<InstantiationAwareBeanPostProcessor> instantiationAware = new ArrayList<>();

		/**
		 * Instantiation 阶段执行的后处理器
		 */
		final List<SmartInstantiationAwareBeanPostProcessor> smartInstantiationAware = new ArrayList<>();

		final List<DestructionAwareBeanPostProcessor> destructionAware = new ArrayList<>();

		/**
		 * @see AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors
		 */
		final List<MergedBeanDefinitionPostProcessor> mergedDefinition = new ArrayList<>();
	}

}
