package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 * @since 2.0
 */
@SuppressWarnings("all")
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/**
	 * Maximum number of suppressed exceptions to preserve.
	 */
	private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;

	/**
	 * 一级缓存
	 * 实例化 的对象都在此
	 * Cache of singleton objects: bean name(键) to bean instance(值).
	 */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/**
	 * 二级缓存
	 * Cache of early singleton objects: bean name to bean instance.
	 */
	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

	/**
	 * 三级缓存
	 * Cache of singleton factories: bean name to ObjectFactory.
	 */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/**
	 * Set of registered singletons, containing the bean names in registration order.
	 */
	private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

	/**
	 * Names of beans that are currently in creation.
	 * -- 当前正在创建的bean的名称。
	 */
	private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/**
	 * Names of beans currently excluded from in creation checks.：当前从创建检查中排除的bean名称。
	 */
	private final Set<String> inCreationCheckExclusions = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/**
	 * Collection of suppressed Exceptions, available for associating related causes.
	 */
	@Nullable
	private Set<Exception> suppressedExceptions;

	/**
	 * Flag that indicates whether we're currently within destroySingletons.
	 * -- 指示我们当前是否在destroySingletons中的标志。
	 *
	 * @see DefaultSingletonBeanRegistry#destroySingletons() 表示当前 bf 正在销毁
	 */
	private boolean singletonsCurrentlyInDestruction = false;

	/**
	 * Disposable bean instances: bean name to disposable instance.
	 *
	 * 析构函数都注册到这里
	 *
	 * @see AbstractBeanFactory#registerDisposableBeanIfNecessary(java.lang.String, java.lang.Object, org.springframework.beans.factory.support.RootBeanDefinition)
	 */
	private final Map<String, Object> disposableBeans = new LinkedHashMap<>();

	/**
	 * Map between containing bean names: bean name to Set of bean names that the bean contains.
	 */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/**
	 * Map between dependent bean names: bean name to Set of dependent bean names.
	 *
	 * 记录依赖当前 beanName 的其他 beanName
	 */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/**
	 * Map between depending bean names: bean name to Set of bean names for the bean's dependencies.
	 *
	 * 记录当前 beanName 依赖的其他 beanName 集合
	 */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);

	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		synchronized (this.singletonObjects) {
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons.
	 *
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.put(beanName, singletonObject);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 *
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			if (!this.singletonObjects.containsKey(beanName)) {
				this.singletonFactories.put(beanName, singletonFactory);
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}

	@Override
	@Nullable
	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * Return the (raw) singleton object registered under the given name. -- 返回以给定名称注册的（原始）单例对象。
	 * <p>
	 * Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * -- 检查已经实例化的单例，并且还允许对当前创建的单例的早期引用（解析循环引用）。
	 *
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not -- 是否允许拿到早期引用
	 * @return the registered singleton object, or {@code null} if none found
	 */
	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// Quick check for existing instance without full singleton lock
		//从一级缓存中拿，第一次拿应该是Null
		Object singletonObject = this.singletonObjects.get(beanName);
		if (
		/**
		 * 一级缓存中没有找到，有几种情况：
		 * 1.该单实例确实没有创建
		 * 2.该单实例正在创建中，当前出现循环依赖了！！！
		 *
		 * 什么是循环依赖？
		 * 1.A依赖B，B依赖A
		 * 2.A依赖B，B依赖C,C依赖A
		 *
		 * 单实例bean有几种循环依赖情况呢？
		 * 1.构造方法注入的时候发生循环依赖，这种情况是无法解决的，spring是直接抛出异常
		 * 2.setter方法注入发生循环依赖，这样情况可以解决，通过三级缓存
		 *
		 * 通过三级缓存如何解决setter循环依赖呢？
		 * 例子A依赖B，B依赖A
		 * 1.实例化A，拿到A的构造方法，通过反射创建A的早期实例，这个早期对象被封装成 ObjectFactory aObjectFactory 对象，放到了三级缓存中{@link DefaultSingletonBeanRegistry#singletonFactories}
		 * 2.处理A的依赖，此时发现A依赖了B，所以接下来就会根据B类型到容器中 getBean(B.class) 获取B的实例，这里就发生了递归
		 * 3.实例化B，拿到B的构造方法，通过反射创建B的早期实例，这个早期对象被封装成 ObjectFactory bObjectFactory 对象，放到了三级缓存中{@link DefaultSingletonBeanRegistry#singletonFactories}
		 * 4.处理B的依赖，此时发现B依赖了A，所以接下来就会根据A类型到容器中 getBean(A.class) 获取A的实例，这里又发生了递归
		 * 5.在第二次试图获取A实例的时候，程序还会来到方法{@link DefaultSingletonBeanRegistry#getSingleton(java.lang.String)}，自然也会来到方法{@link DefaultSingletonBeanRegistry#getSingleton(java.lang.String, boolean)}
		 * 6.此时一级缓存中是没有A实例的(条件一成立)，并且A也是正在创建中的(条件二也成立)
		 * 7.此时二级缓存中也是没有A实例的(第二个if的条件一成立)，并且 allowEarlyReference 也为true
		 * 8.但是此时三级缓存中是又A的早期对象的，此时就会创建出A的一个对象放到二级缓存中，并且返回A对象
		 * 9.递归退出一层，B实例得到了依赖A的一个引用，那么实例B就成功的创建了
		 * 10.递归再退出一层，最终来到了一开始创建A实例的递归堆栈，此时A也成功的拿到了B实例，所以实例A也创建成功
		 */
				singletonObject == null
						/**
						 * 当前 beanName 是否正在创建中？
						 * @see DefaultSingletonBeanRegistry#singletonsCurrentlyInCreation: 一个Set--Set<String> singletonsCurrentlyInCreation
						 */
						&& isSingletonCurrentlyInCreation(beanName)) {

			/**
			 * 如果一级缓存中没有，并且当前beanName正在创建中
			 * 则去二级缓存中看看有没有:Map<String, Object> earlySingletonObjects
			 */
			singletonObject = this.earlySingletonObjects.get(beanName);
			if (singletonObject == null //二级缓存中也没有
					//是否可以创建早期引用？
					&& allowEarlyReference) {

				/**
				 * 如果一级缓存跟二级缓存都没有，并且当前beanName正在创建中，并且可以创建早期引用
				 * 则获取一级缓存map的对象锁监视器
				 */
				synchronized (this.singletonObjects) {//一级缓存加锁
					// Consistent creation of early reference within full singleton lock -- 在完整的单例锁定中一致创建早期引用

					//再次看一级缓存
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {

						//再次看二级缓存：Map<String, Object> earlySingletonObjects
						singletonObject = this.earlySingletonObjects.get(beanName);
						if (singletonObject == null) {

							//最后看三级缓存,三级缓存中存储了 ObjectFactory<?> 可以使用该实例创建当前 beanName 对应的实例：Map<String, ObjectFactory<?>>
							ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
							if (singletonFactory != null) {
								/**
								 * 三级缓存中存储了 ObjectFactory<?> 可以使用该实例创建当前 beanName 对应的实例
								 * @see ObjectFactory#getObject()
								 *
								 * 为什么要三级缓存，如果只有二级缓存的话是不是也能解决问题呢？
								 * 答：
								 * spring除了ioc,还有aop
								 * AOP是通过动态代理实现的，包括(jdk跟cglib2个方式)
								 * 静态代理：需要手动写代码，实现新类，这个类需要和代理对象实现一个接口，内部维护一个被代理(原生)对象，代理类在对应原生对象前后可以添加一些其他逻辑，实现增强，总结：代理对象和被代理对象的内存地址是不一样的
								 * 动态代理：不需要手动写代码，而是依靠字节码框架生成class字节码文件，然后jvm再加载，然后也一样，也是去new代理对象，这个代理对象没有啥特殊的，也是内部保留；额原生对象，然后在调用原生对象前后实现字节码增强
								 *
								 * 三级缓存中保存的是对象工厂(ObjectFactory)，这个工厂内部保留了最原生的对象引用，{@link ObjectFactory#getObject() }方法，它需要考虑一个问题：它到底要返回一个原生的还是一个增强后的对象？
								 * {@link ObjectFactory#getObject()} 方法会判断当前这个早期对象，是否需要被增强，如果要，则提前完成动态代理增强，返回代理对象，否则返回原生对象
								 */
								singletonObject = singletonFactory.getObject();
								//创建成功放入二级缓存
								this.earlySingletonObjects.put(beanName, singletonObject);
								//删除三级缓存
								this.singletonFactories.remove(beanName);
							}
						}
					}
				}
			}
		}
		return singletonObject;
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 *
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 */
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		synchronized (this.singletonObjects) {//一级缓存同步锁

			//从一级缓存中拿，还没有创建的实例是拿不到的
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				//从一级缓存中没有实例

				if (this.singletonsCurrentlyInDestruction) {//如果容器正在销毁，不允许创建实例了
					//只有容器销毁时候该属性会设置为True，此时就不能再创建实例了
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " + "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}

				/**
				 * TODO:做一些检查，可以防止循环依赖，如果发现了循环依赖，则直接抛出异常(该方法里面的逻辑我不是很明白)
				 * 将当前 beanName 放入到 正在创建中的单例集合，放入成功，说明暂时没有循环依赖，如果放入失败，表示产生了循环依赖，里面会抛出异常
				 * @see DefaultSingletonBeanRegistry#singletonsCurrentlyInCreation
				 * 例子：
				 * A 依赖 B，B 依赖 A (都是构造方法依赖)
				 * 1.加载A，{@link DefaultSingletonBeanRegistry#singletonsCurrentlyInCreation} 添加 A，依赖 A
				 * 2.加载B，{@link DefaultSingletonBeanRegistry#singletonsCurrentlyInCreation} 添加 B，依赖 A
				 * 3.再次加载 A,执行到下面的代码 {@link #beforeSingletonCreation} 发现 {@link DefaultSingletonBeanRegistry#singletonsCurrentlyInCreation} 中已经有 A，该方法会抛出异常
				 */
				beforeSingletonCreation(beanName);

				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					/**
					 * singletonFactory 一般是传进来一个匿名内部类
					 * @see AbstractAutowireCapableBeanFactory#createBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[])
					 */
					singletonObject = singletonFactory.getObject();

					//新创建成功
					newSingleton = true;
				} catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				} catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				} finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					//添加到一级缓存，二级缓存更三级缓存都去掉
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}

	/**
	 * Register an exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * <p>The default implementation preserves any given exception in this registry's
	 * collection of suppressed exceptions, up to a limit of 100 exceptions, adding
	 * them as related causes to an eventual top-level {@link BeanCreationException}.
	 *
	 * @param ex the Exception to register
	 * @see BeanCreationException#getRelatedCauses()
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null && this.suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 *
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex()
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	@Override
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}

	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		} else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 *
	 * @param beanName the name of the bean
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation. -- 创建单例之前的回调
	 *
	 * <p>The default implementation register the singleton as currently in creation.
	 * -- 默认实现将单例注册为当前正在创建中。
	 *
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) //该 beanName 是否可以不要检查

				//把beanName 放到 singletonsCurrentlyInCreation(一个并发Map)
				&& !this.singletonsCurrentlyInCreation.add(beanName)) {//该 beanName 是否正在创建中

			//如果创建中，则抛出异常
			throw new BeanCurrentlyInCreationException(beanName);
		}
		//如果不需要检查，或者是第一次创建，则正常完成
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 *
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}

	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 *
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 *
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 *
	 * 假设<bean name = "A" depends-on = "B" .../>
	 * 则 beanName = B, dependentBeanName = A
	 *
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {

		/**
		 * 假设<bean name = "A" depends-on = "B" .../>
		 * 则 beanName = B, dependentBeanName = A
		 */
		String canonicalName = canonicalName(beanName);

		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans = this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean = this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * -- 确定指定的依赖bean是否已注册为依赖于给定bean或其任何传递依赖。
	 *
	 * @param beanName the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	/**
	 * 假设
	 * <bean name = "A" depends-on = "B" .../>
	 * <bean name = "B" depends-on = "A" .../>
	 *
	 * 先处理 A
	 * 再处理 B
	 *
	 * 那么在处理 B 的时候也会来到当前方法
	 * 当前数据情况：
	 *
	 * dependentBeanMap:{"B":{"A}}
	 * dependenciesForBeanMap: {"A" : {"B"}}
	 */
	private boolean isDependent(
			String beanName, // B
			String dependentBeanName, //A

			/**
			 * <bean name = "A" depends-on = "B" .../>
			 * <bean name = "B" depends-on = "C" .../>
			 * <bean name = "C" depends-on = "A" .../>
			 * 如果有这样的场景，则该参数有值
			 */
			@Nullable Set<String> alreadySeen) { // null
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}

		//B
		String canonicalName = canonicalName(beanName);

		//得到 {"A"}
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null) {
			return false;
		}

		//{"A"} .contains("A") == true
		if (dependentBeans.contains(dependentBeanName)) {
			/**
			 * 表示发生了循环依赖
			 * <bean name = "A" depends-on = "B" .../>
			 * <bean name = "B" depends-on = "A" .../>
			 *
			 * 这种情况的循环依赖在这里就能判断出来了
			 */
			return true;
		}

		/**
		 * 但是如果是一个环状的循环依赖，则需要下面的逻辑
		 *
		 * <bean name = "A" depends-on = "B" .../>
		 * <bean name = "B" depends-on = "C" .../>
		 * <bean name = "C" depends-on = "A" .../>
		 *
		 * 假设加载顺序为 A -> B -> C
		 * 则加载到C的时候，数据情况为
		 *
		 * @see DefaultSingletonBeanRegistry#dependentBeanMap 记录依赖当前 beanName 的其他 beanName : {"B":{"A"} , "C":{"B"}}
		 * @see DefaultSingletonBeanRegistry#dependenciesForBeanMap 记录当前 beanName 依赖的其他 beanName 集合 : {}
		 */
		for (String transitiveDependency : dependentBeans) {
			if (alreadySeen == null) {
				alreadySeen = new HashSet<>();
			}
			alreadySeen.add(beanName);
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 *
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 *
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		synchronized (this.dependentBeanMap) {
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 *
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		synchronized (this.dependenciesForBeanMap) {
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		/**
		 * 创建单实例的时候，会检查(在后处理器中实现)当前单实例类型是否实现了DisposableBean接口，如果实现了该接口，
		 * 在容器销毁的时候，需要执行 bean.destroy()方法
		 */
		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		clearSingletonCache();
	}

	/**
	 * Clear all cached singleton instances in this registry.
	 *
	 * @since 4.3.15
	 */
	protected void clearSingletonCache() {
		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 *
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.

		/**
		 * 移除三级缓存
		 * 以及 registeredSingletons 里面对应 beanName 数据
		 */
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			//移除并且拿到引用
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		//执行
		destroyBean(beanName, disposableBean);
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 *
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		Set<String> dependencies;
		synchronized (this.dependentBeanMap) {
			/**
			 * dependentBeanMap：保存的是依赖当前 bean 的其他 bean 信息
			 */
			// Within full synchronization in order to guarantee a disconnected Set
			dependencies = this.dependentBeanMap.remove(beanName);
		}
		if (dependencies != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			for (String dependentBeanName : dependencies) {
				/**
				 * 因为依赖对象要被回收，所以依赖当前bean的对象也要回收
				 */
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		if (bean != null) {
			try {
				//销毁当前bean
				bean.destroy();
			} catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// Trigger destruction of contained beans...
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		/**
		 * dependenciesForBeanMap 保存当前 bean 自己依赖的 beanName
		 * 例子：a 依赖 w,x，那么 dependenciesForBeanMap 中就有key-value:
		 * {a, : {w,x}}
		 */
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Exposes the singleton mutex to subclasses and external collaborators.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	@Override
	public final Object getSingletonMutex() {
		return this.singletonObjects;
	}
}