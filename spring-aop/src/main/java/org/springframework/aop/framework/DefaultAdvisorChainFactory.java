package org.springframework.aop.framework;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 * -- 给定一个{@link Advised}对象，一种为方法制定建议链的简单但确定的方法。始终重建每个建议链；缓存可以由子类提供。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	/**
	 * 查找适合该方法的增强
	 *
	 * @param config the AOP configuration in the form of an Advised object 就是 ProxyFactory(掌握AOP所有生产资料)
	 * @param method the proxied method 目标对象的方法
	 * @param targetClass the target class (may be {@code null} to indicate a proxy without 目标类型
	 * target object, in which case the method's declaring class is the next best option)
	 * @return 增强列表
	 */
	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised config, Method method, @Nullable Class<?> targetClass) {

		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.

		/**
		 * 适配器注册中心
		 * 作用：1.可以注册 AdvisorAdaptor.
		 * 适配器AdvisorAdaptor目的：1.将非Advisor类型的增强包装成为Advisor,2.将Advisor类型的增强提前出来对应的 MethodInterceptor
		 */
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();

		/**
		 * 拿到 ProxyFactory 内部持有的增强信息
		 * 1.addAdvice()
		 * 2.addAdvisor()
		 * 最终在 ProxyFactory 内都会包装成为 Advisor
		 */
		Advisor[] advisors = config.getAdvisors();

		//拦截器列表
		List<Object> interceptorList = new ArrayList<>(advisors.length);

		//真实的目标对象类型
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());

		//引介增强，不关心
		Boolean hasIntroductions = null;

		//遍历所有配置的增强
		for (Advisor advisor : advisors) {
			if (advisor instanceof PointcutAdvisor) {
				//条件成立：说明当前 advisor 是包含切点信息的，所以 if 里面的逻辑，就是做匹配算法
				// Add it conditionally.：有条件地添加它。

				//转换成可以获取到切点信息接口
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				if (config.isPreFiltered()
						||
						/**
						 * 当前被代理对象的class是否匹配该切点
						 *
						 * 再进一步看看方法是否匹配
						 */
						pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {

					//获取方法匹配器
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();

					//表示方法是否匹配
					boolean match;
					if (mm instanceof IntroductionAwareMethodMatcher) {
						//引介不考虑
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
					} else {

						//对应方法匹配函数
						match = mm.matches(method, actualClass);
					}
					if (match) {
						//如果方法匹配成功

						/**
						 * TODO 这里很重要！！！！！
						 * 提前出来当前增强(advisor)的所有拦截器(interceptors)
						 *
						 * @see DefaultAdvisorAdapterRegistry#getInterceptors(org.springframework.aop.Advisor)
						 */
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						if (mm.isRuntime()) {
							//是否需要运行时匹配

							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						} else {
							//将当前 advisor 内部的方法拦截器添加到列表
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}

			//不考虑
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}

			//如果当前增强没有实现PointcutAdvisor，则匹配所有方法
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}

		//返回所有匹配当前方法的拦截器
		return interceptorList;
	}

	/**
	 * Determine whether the Advisors contain matching introductions.
	 */
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}
}