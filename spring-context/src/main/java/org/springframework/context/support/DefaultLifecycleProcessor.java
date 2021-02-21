package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.Phased;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of the {@link LifecycleProcessor} strategy.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(getClass());

	private volatile long timeoutPerShutdownPhase = 30000;

	private volatile boolean running;

	@Nullable
	private volatile ConfigurableListableBeanFactory beanFactory;

	/**
	 * Specify the maximum time allotted in milliseconds for the shutdown of
	 * any phase (group of SmartLifecycle beans with the same 'phase' value).
	 * <p>The default value is 30 seconds.
	 */
	public void setTimeoutPerShutdownPhase(long timeoutPerShutdownPhase) {
		this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"DefaultLifecycleProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	private ConfigurableListableBeanFactory getBeanFactory() {
		ConfigurableListableBeanFactory beanFactory = this.beanFactory;
		Assert.state(beanFactory != null, "No BeanFactory available");
		return beanFactory;
	}

	// Lifecycle implementation

	/**
	 * Start all registered beans that implement {@link Lifecycle} and are <i>not</i>
	 * already running. Any bean that implements {@link SmartLifecycle} will be
	 * started within its 'phase', and all phases will be ordered from lowest to
	 * highest value. All beans that do not implement {@link SmartLifecycle} will be
	 * started in the default phase 0. A bean declared as a dependency of another bean
	 * will be started before the dependent bean regardless of the declared phase.
	 */
	@Override
	public void start() {
		startBeans(false);
		this.running = true;
	}

	/**
	 * Stop all registered beans that implement {@link Lifecycle} and <i>are</i>
	 * currently running. Any bean that implements {@link SmartLifecycle} will be
	 * stopped within its 'phase', and all phases will be ordered from highest to
	 * lowest value. All beans that do not implement {@link SmartLifecycle} will be
	 * stopped in the default phase 0. A bean declared as dependent on another bean
	 * will be stopped before the dependency bean regardless of the declared phase.
	 *
	 * @see AbstractApplicationContext#stop() 容器stop 的时候调用
	 */
	@Override
	public void stop() {
		stopBeans();
		this.running = false;
	}

	@Override
	public void onRefresh() {
		startBeans(true);
		this.running = true;
	}

	@Override
	public void onClose() {
		stopBeans();
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	// Internal helpers

	/**
	 * @param autoStartupOnly 为 true 的时候，表示只启动 {@link SmartLifecycle} ，
	 * 并且这些 SmartLifeCycle 的方法 {@link SmartLifecycle#isAutoStartup()} 必须要返回 true，
	 *
	 * 如果为 false 则确保启动！！！
	 */
	private void startBeans(boolean autoStartupOnly) {

		//key :beanName,value:实例，获取到所有实现了 LifeCycle接口的对象
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();

		/**
		 * 按处理器的 phase(阶段) 分组
		 * 按照key排序，因为他们之间可能有依赖关系，所以他们的执行需要有一定的顺序
		 * phase越小，越先执行
		 */
		Map<Integer, LifecycleGroup> phases = new TreeMap<>();

		/**
		 * 循环map，然后把他们按照 phase 分组
		 * 分组之后放到 phases map中
		 */
		lifecycleBeans.forEach((beanName, bean) -> {
			if (!autoStartupOnly//为 false 说明全部分组，如果为 true 则看后面的条件

					/**
					 * 启动{@link SmartLifecycle}并且{@link SmartLifecycle#isAutoStartup()} 必须要返回 true的 SmartLifecycle
					 */
					|| (bean instanceof SmartLifecycle && ((SmartLifecycle) bean).isAutoStartup())) {

				int phase = getPhase(bean);

				/**
				 * 分组：因为可能有多个 LifeCycle 的 phase 是相同的
				 * 把相同的放到一个 {@link LifecycleGroup}
				 */
				phases.computeIfAbsent(
						phase,//如果当前map中还没有该phase，则添加，否则直接返回之前的value
						p -> new LifecycleGroup(phase, this.timeoutPerShutdownPhase, lifecycleBeans, autoStartupOnly)
				)
						/**
						 * 添加到 {@link LifecycleGroup}中去
						 */
						.add(beanName, bean);
			}
		});

		//按照 phase 分组完成了
		if (!phases.isEmpty()) {
			//一组一组的执行
			phases.values().forEach(LifecycleGroup::start);
		}
	}

	/**
	 * Start the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that it depends on are started first.
	 *
	 * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to start
	 * @param autoStartupOnly
	 */
	private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName, boolean autoStartupOnly) {
		//保证只启动一次，移除之后其他分组中也是看不到的
		Lifecycle bean = lifecycleBeans.remove(beanName);
		if (bean != null && bean != this) {

			//当前要前端的bean依赖的beanName数组
			String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
			for (String dependency : dependenciesForBean) {
				//递归调用,先启动当前bean依赖的bean（当然他们可能并不是一个 LifeCycle）
				doStart(lifecycleBeans, dependency, autoStartupOnly);
			}
			if (!bean.isRunning()//当前要启动的 LifeCycle 还没有启动
					&&
					(!autoStartupOnly || !(bean instanceof SmartLifecycle) || ((SmartLifecycle) bean).isAutoStartup())) {

				if (logger.isTraceEnabled()) {
					logger.trace("Starting bean '" + beanName + "' of type [" + bean.getClass().getName() + "]");
				}
				try {
					bean.start();
				} catch (Throwable ex) {
					throw new ApplicationContextException("Failed to start bean '" + beanName + "'", ex);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Successfully started bean '" + beanName + "'");
				}
			}
		}
	}

	private void stopBeans() {
		//大体上跟 startBean 类似
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, LifecycleGroup> phases = new HashMap<>();
		lifecycleBeans.forEach((beanName, bean) -> {
			int shutdownPhase = getPhase(bean);
			LifecycleGroup group = phases.get(shutdownPhase);
			if (group == null) {
				group = new LifecycleGroup(shutdownPhase, this.timeoutPerShutdownPhase, lifecycleBeans, false);
				phases.put(shutdownPhase, group);
			}
			group.add(beanName, bean);
		});
		if (!phases.isEmpty()) {
			List<Integer> keys = new ArrayList<>(phases.keySet());
			//跟 start 的顺序相反,phase越小，执行越靠后
			keys.sort(Collections.reverseOrder());
			for (Integer key : keys) {
				phases.get(key).stop();
			}
		}
	}

	/**
	 * Stop the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that depends on it are stopped first.
	 *
	 * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to stop
	 */
	private void doStop(
			Map<String, ? extends Lifecycle> lifecycleBeans,
			final String beanName,
			final CountDownLatch latch,
			final Set<String> countDownBeanNames) {

		Lifecycle bean = lifecycleBeans.remove(beanName);
		if (bean != null) {
			String[] dependentBeans = getBeanFactory().getDependentBeans(beanName);
			for (String dependentBean : dependentBeans) {
				/**
				 * 关闭依赖当前beanName的其他 beanName，并且他们要先于当前对象关闭
				 */
				doStop(lifecycleBeans, dependentBean, latch, countDownBeanNames);
			}
			try {
				if (bean.isRunning()) {//是 running
					if (bean instanceof SmartLifecycle) {
						//SmartLifecycle 类型
						if (logger.isTraceEnabled()) {
							logger.trace("Asking bean '" + beanName + "' of type [" +
									bean.getClass().getName() + "] to stop");
						}
						//将当前 SmartLifecycle 的 bn 添加到 countDownBeanNames 集合，countDownBeanNames集合表示正在关闭的 SmartLifecycle
						countDownBeanNames.add(beanName);
						((SmartLifecycle) bean).stop(() -> {
							latch.countDown();

							//关闭完成
							countDownBeanNames.remove(beanName);
							if (logger.isDebugEnabled()) {
								logger.debug("Bean '" + beanName + "' completed its stop procedure");
							}
						});
					} else {

						//普通的 LifeCycle
						if (logger.isTraceEnabled()) {
							logger.trace("Stopping bean '" + beanName + "' of type [" +
									bean.getClass().getName() + "]");
						}

						//普通的 LifeCycle
						bean.stop();
						if (logger.isDebugEnabled()) {
							logger.debug("Successfully stopped bean '" + beanName + "'");
						}
					}
				} else if (bean instanceof SmartLifecycle) {
					// Don't wait for beans that aren't running...
					latch.countDown();
				}
			} catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to stop bean '" + beanName + "'", ex);
				}
			}
		}
	}

	// overridable hooks

	/**
	 * Retrieve all applicable Lifecycle beans:
	 * --检索所有适用的Lifecycle bean：
	 *
	 * all singletons that have already been created,
	 * as well as all SmartLifecycle beans (even if they are marked as lazy-init).
	 *
	 * @return the Map of applicable beans, with bean names as keys and bean instances as values
	 */
	protected Map<String, Lifecycle> getLifecycleBeans() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		Map<String, Lifecycle> beans = new LinkedHashMap<>();
		String[] beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
		for (String beanName : beanNames) {
			String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
			boolean isFactoryBean = beanFactory.isFactoryBean(beanNameToRegister);
			String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
			if ((
					beanFactory.containsSingleton(beanNameToRegister)
							&&
							(!isFactoryBean || matchesBeanType(Lifecycle.class, beanNameToCheck, beanFactory))
			)

					||

					matchesBeanType(SmartLifecycle.class, beanNameToCheck, beanFactory)) {

				Object bean = beanFactory.getBean(beanNameToCheck);
				if (bean != this && bean instanceof Lifecycle) {
					beans.put(beanNameToRegister, (Lifecycle) bean);
				}
			}
		}
		return beans;
	}

	private boolean matchesBeanType(Class<?> targetType, String beanName, BeanFactory beanFactory) {
		Class<?> beanType = beanFactory.getType(beanName);
		return (beanType != null && targetType.isAssignableFrom(beanType));
	}

	/**
	 * Determine the lifecycle phase of the given bean.
	 * <p>The default implementation checks for the {@link Phased} interface, using
	 * a default of 0 otherwise. Can be overridden to apply other/further policies.
	 *
	 * @param bean the bean to introspect
	 * @return the phase (an integer value)
	 * @see Phased#getPhase()
	 * @see SmartLifecycle
	 */
	protected int getPhase(Lifecycle bean) {
		return (
				bean instanceof Phased ?
						((Phased) bean).getPhase()
						: 0
		);
	}

	/**
	 * Helper class for maintaining a group of Lifecycle beans that should be started
	 * and stopped together based on their 'phase' value (or the default value of 0).
	 */
	private class LifecycleGroup {

		//当前分组的执行阶段（顺序）
		private final int phase;

		/**
		 * 超时
		 * @see DefaultLifecycleProcessor#timeoutPerShutdownPhase
		 */
		private final long timeout;

		//所有的 lifeCycle 的实现
		private final Map<String, ? extends Lifecycle> lifecycleBeans;

		//是否自动启动
		private final boolean autoStartupOnly;

		/**
		 * 当前分组的成员
		 */
		private final List<LifecycleGroupMember> members = new ArrayList<>();

		/**
		 * @see LifecycleGroup#add(java.lang.String, org.springframework.context.Lifecycle) 只统计 {@link SmartLifecycle} 类型的
		 */
		private int smartMemberCount;

		public LifecycleGroup(int phase, long timeout, Map<String, ? extends Lifecycle> lifecycleBeans, boolean autoStartupOnly) {

			this.phase = phase;
			this.timeout = timeout;
			this.lifecycleBeans = lifecycleBeans;
			this.autoStartupOnly = autoStartupOnly;
		}

		public void add(String name, Lifecycle bean) {
			this.members.add(new LifecycleGroupMember(name, bean));
			if (bean instanceof SmartLifecycle) {
				this.smartMemberCount++;
			}
		}

		public void start() {
			if (this.members.isEmpty()) {
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Starting beans in phase " + this.phase);
			}
			Collections.sort(this.members);
			for (LifecycleGroupMember member : this.members) {
				//遍历，然后启动
				doStart(this.lifecycleBeans, member.name, this.autoStartupOnly);
			}
		}

		public void stop() {
			if (this.members.isEmpty()) {
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Stopping beans in phase " + this.phase);
			}
			this.members.sort(Collections.reverseOrder());
			/**
			 * 得到当前分组中的所有Smart类型的实例都stop完成
			 */
			CountDownLatch latch = new CountDownLatch(this.smartMemberCount);
			//保存当前正在处于关闭ing状态的SmartLifeCycle的 beanName
			Set<String> countDownBeanNames = Collections.synchronizedSet(new LinkedHashSet<>());

			//bf 全部 LifeCycle 的 beanNames
			Set<String> lifecycleBeanNames = new HashSet<>(this.lifecycleBeans.keySet());
			for (LifecycleGroupMember member : this.members) {//遍历当前分组的成员
				if (lifecycleBeanNames.contains(member.name)) {
					doStop(this.lifecycleBeans, member.name, latch, countDownBeanNames);
				} else if (member.bean instanceof SmartLifecycle) {
					// Already removed: must have been a dependent bean from another phase
					// 已经删除：必须是另一个阶段的从属bean
					latch.countDown();
				}
			}
			try {
				/**
				 * 主线程在此等待latch归零或者超时
				 * @see DefaultLifecycleProcessor#timeoutPerShutdownPhase
				 */
				latch.await(this.timeout, TimeUnit.MILLISECONDS);
				if (latch.getCount() > 0 && !countDownBeanNames.isEmpty() && logger.isInfoEnabled()) {
					logger.info("Failed to shut down " + countDownBeanNames.size() + " bean" +
							(countDownBeanNames.size() > 1 ? "s" : "") + " with phase value " +
							this.phase + " within timeout of " + this.timeout + "ms: " + countDownBeanNames);
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Adapts the Comparable interface onto the lifecycle phase model.
	 */
	private class LifecycleGroupMember implements Comparable<LifecycleGroupMember> {

		private final String name;

		private final Lifecycle bean;

		LifecycleGroupMember(String name, Lifecycle bean) {
			this.name = name;
			this.bean = bean;
		}

		@Override
		public int compareTo(LifecycleGroupMember other) {
			int thisPhase = getPhase(this.bean);
			int otherPhase = getPhase(other.bean);
			return Integer.compare(thisPhase, otherPhase);
		}
	}
}