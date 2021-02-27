package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @see AnnotationAwareAspectJAutoProxyCreator
 * @since 2.0.2
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;

	@Nullable
	private volatile List<String> aspectBeanNames;

	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 *
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 *
	 * @param beanFactory the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}

	/**
	 * 由 @Aspect 注解提供的增强，由当前方法构建
	 *
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 *
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 * @see AnnotationAwareAspectJAutoProxyCreator#findCandidateAdvisors() 该方法调用当前方法
	 */
	public List<Advisor> buildAspectJAdvisors() {
		List<String> aspectNames = this.aspectBeanNames;

		if (aspectNames == null) {
			//第一次来，缓存中没有 advisor 信息，需要一个一个的提取出来
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {

					//保存查询结果 @Aspect
					List<Advisor> advisors = new ArrayList<>();

					//添加  @Aspect  的 BEAnName
					aspectNames = new ArrayList<>();

					//获取 beanFactory 中全部 beanName
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, Object.class, true, false);
					for (String beanName : beanNames) {
						if (!isEligibleBean(beanName)) {
							continue;
						}

						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
						Class<?> beanType = this.beanFactory.getType(beanName, false);
						if (beanType == null) {
							//当前 bean 标签是 abstract = true 的适合，或者 是 parent
							continue;
						}

						/**
						 * @see ReflectiveAspectJAdvisorFactory 该类型实例
						 * @see AbstractAspectJAdvisorFactory#isAspect(java.lang.Class)
						 */
						if (this.advisorFactory.isAspect(beanType)) {
							// 表示当前类型以及父类..有 @Aspect 注解

							aspectNames.add(beanName);

							//Aspect 元数据
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
								//正常情况下，都是 SINGLETON

								//使用工厂模式管理 Aspect 元数据 关联 的 真实的 @Aspect 注解的 实例对象
								MetadataAwareAspectInstanceFactory factory = new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);

								/**
								 * @see ReflectiveAspectJAdvisorFactory#getAdvisors(MetadataAwareAspectInstanceFactory) TODO
								 * 这个方法返回指定添加类 @Aspect 注解的 class 相关的 Advisor 信息
								 */
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);

								//缓存机制
								if (this.beanFactory.isSingleton(beanName)) {
									this.advisorsCache.put(beanName, classAdvisors);
								} else {
									this.aspectFactoryCache.put(beanName, factory);
								}
								advisors.addAll(classAdvisors);
							}

							//不考虑，属于 aspect 高级用法
							else {
								// Per target or per this.
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName + "' is a singleton, but aspect instantiation model is not singleton");
								}
								MetadataAwareAspectInstanceFactory factory = new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								this.aspectFactoryCache.put(beanName, factory);
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}

					//缓存
					this.aspectBeanNames = aspectNames;
					return advisors;
				}
			}
		}

		//缓存有数据

		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			} else {
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 *
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}
}