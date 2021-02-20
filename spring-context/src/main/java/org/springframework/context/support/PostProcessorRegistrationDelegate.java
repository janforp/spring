package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}

	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory,
			/**
			 * 硬编码传进来的bfpp
			 */
			List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.

		/**
		 * 存储已经执行过的 bfpp 的 beanName
		 */
		Set<String> processedBeans = new HashSet<>();

		if (beanFactory instanceof BeanDefinitionRegistry) {
			//条件成立，说明 beanFactory 是 BeanDefinitionRegistry 类型的
			/**
			 * @see DefaultListableBeanFactory 大部分
			 */

			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			/**
			 * 常规后处理器
			 * 通过下面的循环塞入值
			 * 后面会统一执行一些方法
			 */
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();

			/**
			 * 存储的 BeanFactoryPostProcessor 是一个  BeanDefinitionRegistryPostProcessor
			 *
			 * BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor
			 *
			 * 这种类型的容器后处理器，可以再向容器内手动硬编码注册一些bd
			 */
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			//处理 applicationContext 上面硬编码注册的 bfpp 处理器
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					//如果是 BeanDefinitionRegistryPostProcessor 实现

					//转成 BeanDefinitionRegistryPostProcessor
					BeanDefinitionRegistryPostProcessor registryProcessor = (BeanDefinitionRegistryPostProcessor) postProcessor;

					/**
					 * 直接调用 {@link BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(org.springframework.beans.factory.support.BeanDefinitionRegistry)} 方法
					 *
					 * @see BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(org.springframework.beans.factory.support.BeanDefinitionRegistry)
					 */
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					//添加到注册类型的集合
					registryProcessors.add(registryProcessor);
				}

				//如果不是 BeanDefinitionRegistryPostProcessor 实现，则是普通的bfpp
				else {
					//添加到普通类型集合，后续会统一执行一些方法
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			//不要在这里初始化FactoryBeans：我们需要保留所有未初始化的常规bean，以使bean工厂后处理器对其应用！在实现PriorityOrdered，Ordered和其余优先级的BeanDefinitionRegistryPostProcessor之间分开。

			/**
			 * 当前阶段的 registry 后处理器集合
			 * @see PriorityOrdered
			 * @see Ordered
			 */
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.:首先，调用实现PriorityOrdered的BeanDefinitionRegistryPostProcessors。

			String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//当前的 ppName 对应的 BeanDefinitionRegistryPostProcessor 实现了 PriorityOrdered(主排序接口)

					currentRegistryProcessors.add(

							/**
							 * 获取bf获取bean
							 */
							beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class)
					);

					//已经执行过的集合:因为马上要执行相关接口方法了，所以添加到这里。表示已经执行
					processedBeans.add(ppName);
				}
			}

			/**
			 * 排序 pp
			 * @see OrderComparator#doCompare(java.lang.Object, java.lang.Object, org.springframework.core.OrderComparator.OrderSourceProvider)
			 */
			sortPostProcessors(currentRegistryProcessors, beanFactory);

			//添加到 registry 后处理器集合内
			registryProcessors.addAll(currentRegistryProcessors);

			/**
			 * 执行每个bdrpp的方法
			 * @see PostProcessorRegistrationDelegate#invokeBeanDefinitionRegistryPostProcessors(java.util.Collection, org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.core.metrics.ApplicationStartup)
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			/**
			 * 执行完成，清理，后面还要用呢
			 */
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.：接下来，调用实现Ordered的BeanDefinitionRegistryPostProcessors。

			/**
			 * 复用变量名称
			 */
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) //还没有执行，目的是确保每个bdrpp只执行一次

						&& beanFactory.isTypeMatch(ppName, Ordered.class)) {
					//当前的 ppName 对应的 BeanDefinitionRegistryPostProcessor 实现了 Ordered 并且还没有执行过

					/**
					 * 复用 空集合 currentRegistryProcessors
					 */
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			/**
			 * 排序 pp
			 * @see OrderComparator#doCompare(java.lang.Object, java.lang.Object, org.springframework.core.OrderComparator.OrderSourceProvider)
			 */
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			/**
			 * 执行 postProcessBeanDefinitionRegistry 方法
			 * @see PostProcessorRegistrationDelegate#invokeBeanDefinitionRegistryPostProcessors(java.util.Collection, org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.core.metrics.ApplicationStartup)
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			/**
			 * 执行完成，清理，后面还要用呢
			 */
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.：最后，调用所有其他BeanDefinitionRegistryPostProcessor，直到没有其他的出现。

			boolean reiterate /*控制while是否需要再次循环*/ = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						//如果还没执行，则添加到集合

						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);

						/**
						 * 为何设置true？
						 * 因为 bdrpp 可以向 bf 中注册新的bd,而这些 bd 也有可能是 BeanDefinitionRegistryPostProcessor 类型的，所以需要再次循环
						 * @see BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry(org.springframework.beans.factory.support.BeanDefinitionRegistry)
						 * 该方法就很有可能注册该类型的bean
						 */
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);

				/**
				 * 此处注册bd，可能会注册新的 BeanDefinitionRegistryPostProcessor
				 */
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				/**
				 * 执行完成，清理，后面还要用呢
				 */
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.:现在，调用到目前为止已处理的所有处理器的postProcessBeanFactory回调。

			/**
			 * BeanDefinitionRegistryPostProcessor 其实也是一个 BeanFactoryPostProcessor,BeanFactoryPostProcessor 中的方法也是要执行的
			 *
			 * @see BeanDefinitionRegistryPostProcessor 该接口中的新定义的方法在前面已经执行过了
			 * @see BeanFactoryPostProcessor 该接口中定义的方法还没有执行
			 * @see BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory) 执行这个方法
			 */
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			/**
			 * regularPostProcessors 中存储的是 BeanFactoryPostProcessor 类型的，并不是 BeanDefinitionRegistryPostProcessor 类型的
			 */
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		//beanFactory instanceof BeanDefinitionRegistry 该添加不满足就会执行这个分支
		else {
			// Invoke factory processors registered with the context instance.:调用在上下文实例中注册的工厂处理器

			/**
			 * @see BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory) 执行这个方法
			 */
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		/**
		 * 上面的代码处理了硬编码提供的bfpp以及bdrpp这两种类型的bfpp
		 * 还有普通的bfpp需要处理
		 */

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		// 不要在这里初始化FactoryBeans：我们需要保留所有未初始化的常规bean，以使bean工厂后处理器对其应用！

		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered, Ordered, and the rest.
		//在实现PriorityOrdered，Ordered和其余优先级的BeanFactoryPostProcessor之间分开。

		//主排序处理器集合
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();

		//次排序
		List<String> orderedPostProcessorNames = new ArrayList<>();

		//未排序
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		/**
		 * 筛选出没有执行的bfpp的 beanName，并且归类到上面3个集合中
		 */
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				//ignore：避免重复执行
				// skip - already processed in first phase above
			} else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			} else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			//beanName 转 bean
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			//beanName 转 bean
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, //bf
			AbstractApplicationContext applicationContext)  // ac
	{

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		/**
		 * 获取所有的 BeanPostProcessor
		 */
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.

		/**
		 * @see AbstractBeanFactory#beanPostProcessors
		 */
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;

		/**
		 * @see AbstractBeanFactory#beanPostProcessors 添加到该集合
		 */
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,Ordered, and the rest.
		// 实现PriorityOrdered，Ordered的BeanPostProcessor与其余的分开。

		//主
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		/**
		 * 内部
		 * @see MergedBeanDefinitionPostProcessor 都是该类型的
		 */
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		//次
		List<String> orderedPostProcessorNames = new ArrayList<>();
		//无排序的
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		//遍历所有的bpp的 beanName
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				//如果是 PriorityOrdered 的
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					//如果是 PriorityOrdered 子类，并且还是 MergedBeanDefinitionPostProcessor 子类，则为内部
					internalPostProcessors.add(pp);
				}
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				//如果是 Ordered 的

				orderedPostProcessorNames.add(ppName);
			} else {
				//没有排序的
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.

		//首先，注册实现PriorityOrdered的BeanPostProcessor。

		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		/**
		 * 其实就是按顺序添加到 bf 的一个字段{@link AbstractBeanFactory#beanPostProcessors} 添加到该集合中了
		 */
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		// 接下来，注册实现Ordered的BeanPostProcessor。

		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		//现在，注册所有常规BeanPostProcessor。

		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		// 最后，重新注册所有内部BeanPostProcessor。注册了2次哦，因为两个接口类型，虽然最顶层的接口是一样的，但是方法是不一样的
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		// 重新注册后处理器以将内部bean检测为ApplicationListener，并将其移至处理器链的末尾（用于拾取代理等）。
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors,
			BeanDefinitionRegistry registry,
			ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {

			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process").tag("postProcessor", postProcessor::toString);

			/**
			 * 执行
			 */
			postProcessor.postProcessBeanDefinitionRegistry(registry);

			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors,
			ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {

			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process").tag("postProcessor", postProcessor::toString);

			/**
			 * 执行
			 */
			postProcessor.postProcessBeanFactory(beanFactory);

			postProcessBeanFactory.end();
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 *
	 * 注册 bpp 到 bf 中
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory,
			List<BeanPostProcessor> postProcessors) {

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			// 批量添加对我们的CopyOnWriteArrayList更有效
			/**
			 * @see AbstractBeanFactory#beanPostProcessors 添加到该集合中了
			 */
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		} else {
			for (BeanPostProcessor postProcessor : postProcessors) {

				/**
				 * 循环单个添加
				 * @see AbstractBeanFactory#beanPostProcessors 添加到该集合中了
				 */
				beanFactory.addBeanPostProcessor(postProcessor);
			}
		}
	}

	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 *
	 * -- 当在BeanPostProcessor实例化期间创建Bean时，
	 * 即当某个Bean不适合所有BeanPostProcessor处理时，
	 * 将记录一条信息消息的BeanPostProcessor。
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (
					!(bean instanceof BeanPostProcessor) //当前bean不是 bpp
							&& !isInfrastructureBean(beanName)  //当前beanName 不是基础设施bean

							//当前 bpp 的 beanPostProcessorTargetCount 数量大于 beanFactory 中的 bpp 的数量
							&& this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount
			) {
				if (logger.isInfoEnabled()) {
					//eligible:有资格的
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		/**
		 * Infrastructure：基础设施
		 */
		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}
}