package org.springframework.beans.factory.support;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * Simple object instantiation strategy for use in a BeanFactory.
 * - 在BeanFactory中使用的简单对象实例化策略。
 *
 * <p>Does not support Method Injection, although it provides hooks for subclasses
 * to override to add Method Injection support, for example by overriding methods.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public class SimpleInstantiationStrategy implements InstantiationStrategy {

	private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<>();

	/**
	 * Return the factory method currently being invoked or {@code null} if none.
	 * <p>Allows factory method implementations to determine whether the current
	 * caller is the container itself as opposed to user code.
	 */
	@Nullable
	public static Method getCurrentlyInvokedFactoryMethod() {
		return currentlyInvokedFactoryMethod.get();
	}

	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		// Don't override the class with CGLIB if no overrides.
		if (!bd.hasMethodOverrides()) {
			//一般是没有覆盖方法的

			//默认构造方法
			Constructor<?> constructorToUse;
			synchronized (bd.constructorArgumentLock) {

				//首次实例化是没有缓存的，首次过后就会把构造方法缓存到对应的bd，第二次创建的时候才有缓存
				constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse == null) {
					final Class<?> clazz = bd.getBeanClass();
					if (clazz.isInterface()) {
						throw new BeanInstantiationException(clazz, "Specified class is an interface");
					}
					try {
						if (System.getSecurityManager() != null) {
							constructorToUse = AccessController.doPrivileged((PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor);
						} else {
							/**
							 * @see Class#getDeclaredConstructor(java.lang.Class[]) 获取指定参数类型的构造器
							 * @see Class#getDeclaredConstructors() 获取所有的构造器
							 *
							 * 此处获取指定无参数的构造器，就是默认构造方法
							 */

							//拿到默认的构造方法
							constructorToUse = clazz.getDeclaredConstructor();
						}

						//缓存到bd，方便后面直接使用
						bd.resolvedConstructorOrFactoryMethod = constructorToUse;
					} catch (Throwable ex) {
						throw new BeanInstantiationException(clazz, "No default constructor found", ex);
					}
				}
			}

			//使用反射创建实例
			return BeanUtils.instantiateClass(constructorToUse);
		} else {
			/**
			 * Must generate CGLIB subclass. ---- 必须生成CGLIB子类。
			 *
			 * 如果有 方法 覆盖的情况，则使用 cglib 生成代理对象！！！！
			 * 但是aop默认是使用jdk动态代理！
			 */
			return instantiateWithMethodInjection(bd, beanName, owner);
		}
	}

	/**
	 * Subclasses can override this method, which is implemented to throw
	 * UnsupportedOperationException, if they can instantiate an object with
	 * the Method Injection specified in the given RootBeanDefinition.
	 * Instantiation should use a no-arg constructor.
	 */
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {

		//cglib 实现覆盖了该方法
		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner, final Constructor<?> ctor, Object... args) {
		if (!bd.hasMethodOverrides()) {
			//没有要覆盖方法

			if (System.getSecurityManager() != null) {
				// use own privileged to change accessibility (when security is on)
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(ctor);
					return null;
				});
			}
			return BeanUtils.instantiateClass(ctor, args);
		} else {
			//方法注入
			return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
		}
	}

	/**
	 * Subclasses can override this method, which is implemented to throw
	 * UnsupportedOperationException, if they can instantiate an object with
	 * the Method Injection specified in the given RootBeanDefinition.
	 * Instantiation should use the given constructor and parameters.
	 */
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner, @Nullable Constructor<?> ctor, Object... args) {

		//cglib 实现覆盖了该方法
		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			@Nullable Object factoryBean, final Method factoryMethod, Object... args) {

		try {
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(factoryMethod);
					return null;
				});
			} else {
				ReflectionUtils.makeAccessible(factoryMethod);
			}

			Method priorInvokedFactoryMethod = currentlyInvokedFactoryMethod.get();
			try {
				currentlyInvokedFactoryMethod.set(factoryMethod);
				Object result = factoryMethod.invoke(factoryBean, args);
				if (result == null) {
					result = new NullBean();
				}
				return result;
			} finally {
				if (priorInvokedFactoryMethod != null) {
					currentlyInvokedFactoryMethod.set(priorInvokedFactoryMethod);
				} else {
					currentlyInvokedFactoryMethod.remove();
				}
			}
		} catch (IllegalArgumentException ex) {
			throw new BeanInstantiationException(factoryMethod,
					"Illegal arguments to factory method '" + factoryMethod.getName() + "'; " +
							"args: " + StringUtils.arrayToCommaDelimitedString(args), ex);
		} catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(factoryMethod,
					"Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
		} catch (InvocationTargetException ex) {
			String msg = "Factory method '" + factoryMethod.getName() + "' threw exception";
			if (bd.getFactoryBeanName() != null && owner instanceof ConfigurableBeanFactory &&
					((ConfigurableBeanFactory) owner).isCurrentlyInCreation(bd.getFactoryBeanName())) {
				msg = "Circular reference involving containing bean '" + bd.getFactoryBeanName() + "' - consider " +
						"declaring the factory method as static for independence from its containing instance. " + msg;
			}
			throw new BeanInstantiationException(factoryMethod, msg, ex.getTargetException());
		}
	}
}