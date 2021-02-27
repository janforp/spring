package org.springframework.aop.aspectj.annotation;

import org.aopalliance.aop.Advice;
import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJPrecedenceInformation;
import org.springframework.aop.aspectj.InstantiationModelAwarePointcutAdvisor;
import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactory.AspectJAnnotation;
import org.springframework.aop.support.DynamicMethodMatcherPointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Internal implementation of AspectJPointcutAdvisor.
 * Note that there will be one instance of this advisor for each target method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
final class InstantiationModelAwarePointcutAdvisorImpl
		implements InstantiationModelAwarePointcutAdvisor, AspectJPrecedenceInformation, Serializable {

	private static final Advice EMPTY_ADVICE = new Advice() {
	};

	private final AspectJExpressionPointcut declaredPointcut;

	private final Class<?> declaringClass;

	private final String methodName;

	private final Class<?>[] parameterTypes;

	private transient Method aspectJAdviceMethod;

	private final AspectJAdvisorFactory aspectJAdvisorFactory;

	private final MetadataAwareAspectInstanceFactory aspectInstanceFactory;

	private final int declarationOrder;

	private final String aspectName;

	private final Pointcut pointcut;

	private final boolean lazy;

	/**
	 * 创建出来 Advice 对象，Advice 和 Advisor 的关系是 一一对应的关系，Spring 中每个 Advisor 内部一定持有一个 Advice
	 */
	@Nullable
	private Advice instantiatedAdvice;

	@Nullable
	private Boolean isBeforeAdvice;

	@Nullable
	private Boolean isAfterAdvice;

	/**
	 * @see ReflectiveAspectJAdvisorFactory#getAdvisor(java.lang.reflect.Method, org.springframework.aop.aspectj.annotation.MetadataAwareAspectInstanceFactory, int, java.lang.String)
	 */
	public InstantiationModelAwarePointcutAdvisorImpl(
			AspectJExpressionPointcut declaredPointcut,//切点表达式
			Method aspectJAdviceMethod, //当前方法，内部会把当前方法封装成为 advice
			AspectJAdvisorFactory aspectJAdvisorFactory,//ReflectiveAspectJAdvisorFactory:构造 Advisor 工厂
			MetadataAwareAspectInstanceFactory aspectInstanceFactory,//元数据工厂，通过它可以拿到 @Aspect 注解实例/元数据
			int declarationOrder, //0
			String aspectName) {//名称

		this.declaredPointcut = declaredPointcut;
		this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
		this.methodName = aspectJAdviceMethod.getName();

		//当前方法参数类型数组
		this.parameterTypes = aspectJAdviceMethod.getParameterTypes();

		//增强逻辑方法，也就是哪些添加类 @Before等注解的方法
		this.aspectJAdviceMethod = aspectJAdviceMethod;
		this.aspectJAdvisorFactory = aspectJAdvisorFactory;
		this.aspectInstanceFactory = aspectInstanceFactory;
		this.declarationOrder = declarationOrder;
		this.aspectName = aspectName;

		if (aspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
			//AOP高级应用进入该分支，正常我们不会使用

			// Static part of the pointcut is a lazy type.
			Pointcut preInstantiationPointcut = Pointcuts.union(
					aspectInstanceFactory.getAspectMetadata().getPerClausePointcut(), this.declaredPointcut);

			// Make it dynamic: must mutate from pre-instantiation to post-instantiation state.
			// If it's not a dynamic pointcut, it may be optimized out
			// by the Spring AOP infrastructure after the first evaluation.
			this.pointcut = new PerTargetInstantiationModelPointcut(
					this.declaredPointcut, preInstantiationPointcut, aspectInstanceFactory);
			this.lazy = true;
		} else {
			//我们一般进入这个分支

			// A singleton aspect.
			this.pointcut = this.declaredPointcut;
			this.lazy = false;

			//创建出来 Advice 对象，Advice 和 Advisor 的关系是 一一对应的关系，Spring 中每个 Advisor 内部一定持有一个 Advice
			this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
		}
	}

	/**
	 * The pointcut for Spring AOP to use.
	 * Actual behaviour of the pointcut will change depending on the state of the advice.
	 */
	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

	@Override
	public boolean isLazy() {
		return this.lazy;
	}

	@Override
	public synchronized boolean isAdviceInstantiated() {
		return (this.instantiatedAdvice != null);
	}

	/**
	 * Lazily instantiate advice if necessary.
	 */
	@Override
	public synchronized Advice getAdvice() {
		if (this.instantiatedAdvice == null) {
			this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
		}
		return this.instantiatedAdvice;
	}

	private Advice instantiateAdvice(AspectJExpressionPointcut pointcut) {

		/**
		 * @see ReflectiveAspectJAdvisorFactory#getAdvice(java.lang.reflect.Method, org.springframework.aop.aspectj.AspectJExpressionPointcut, org.springframework.aop.aspectj.annotation.MetadataAwareAspectInstanceFactory, int, java.lang.String)
		 */
		Advice advice = this.aspectJAdvisorFactory.getAdvice(
				this.aspectJAdviceMethod,//增强逻辑方法，也就是哪些添加类 @Before等注解的方法
				pointcut, // AspectJExpressionPointcut 增强逻辑方法注解上的切点表达式
				this.aspectInstanceFactory,
				this.declarationOrder,
				this.aspectName);

		/**
		 * advice 内容最重要的数据 是 当前 method ，因为增强逻辑，最终还是要通过反射的方式调用当前方法的
		 * 还有 aspectInstanceFactory 通过该字段能够获取 @Aspect 对应的类的实例
		 * 只有有了方法跟实例才能反射调用
		 */
		return (advice != null ? advice : EMPTY_ADVICE);
	}

	/**
	 * This is only of interest for Spring AOP: AspectJ instantiation semantics
	 * are much richer. In AspectJ terminology, all a return of {@code true}
	 * means here is that the aspect is not a SINGLETON.
	 */
	@Override
	public boolean isPerInstance() {
		return (getAspectMetadata().getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON);
	}

	/**
	 * Return the AspectJ AspectMetadata for this advisor.
	 */
	public AspectMetadata getAspectMetadata() {
		return this.aspectInstanceFactory.getAspectMetadata();
	}

	public MetadataAwareAspectInstanceFactory getAspectInstanceFactory() {
		return this.aspectInstanceFactory;
	}

	public AspectJExpressionPointcut getDeclaredPointcut() {
		return this.declaredPointcut;
	}

	@Override
	public int getOrder() {
		return this.aspectInstanceFactory.getOrder();
	}

	@Override
	public String getAspectName() {
		return this.aspectName;
	}

	@Override
	public int getDeclarationOrder() {
		return this.declarationOrder;
	}

	@Override
	public boolean isBeforeAdvice() {
		if (this.isBeforeAdvice == null) {
			determineAdviceType();
		}
		return this.isBeforeAdvice;
	}

	@Override
	public boolean isAfterAdvice() {
		if (this.isAfterAdvice == null) {
			determineAdviceType();
		}
		return this.isAfterAdvice;
	}

	/**
	 * Duplicates some logic from getAdvice, but importantly does not force
	 * creation of the advice.
	 */
	private void determineAdviceType() {
		AspectJAnnotation<?> aspectJAnnotation =
				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(this.aspectJAdviceMethod);
		if (aspectJAnnotation == null) {
			this.isBeforeAdvice = false;
			this.isAfterAdvice = false;
		} else {
			switch (aspectJAnnotation.getAnnotationType()) {
				case AtPointcut:
				case AtAround:
					this.isBeforeAdvice = false;
					this.isAfterAdvice = false;
					break;
				case AtBefore:
					this.isBeforeAdvice = true;
					this.isAfterAdvice = false;
					break;
				case AtAfter:
				case AtAfterReturning:
				case AtAfterThrowing:
					this.isBeforeAdvice = false;
					this.isAfterAdvice = true;
					break;
			}
		}
	}

	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		try {
			this.aspectJAdviceMethod = this.declaringClass.getMethod(this.methodName, this.parameterTypes);
		} catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Failed to find advice method on deserialization", ex);
		}
	}

	@Override
	public String toString() {
		return "InstantiationModelAwarePointcutAdvisor: expression [" + getDeclaredPointcut().getExpression() +
				"]; advice method [" + this.aspectJAdviceMethod + "]; perClauseKind=" +
				this.aspectInstanceFactory.getAspectMetadata().getAjType().getPerClause().getKind();
	}

	/**
	 * Pointcut implementation that changes its behaviour when the advice is instantiated.
	 * Note that this is a <i>dynamic</i> pointcut; otherwise it might be optimized out
	 * if it does not at first match statically.
	 */
	private static final class PerTargetInstantiationModelPointcut extends DynamicMethodMatcherPointcut {

		private final AspectJExpressionPointcut declaredPointcut;

		private final Pointcut preInstantiationPointcut;

		@Nullable
		private LazySingletonAspectInstanceFactoryDecorator aspectInstanceFactory;

		public PerTargetInstantiationModelPointcut(AspectJExpressionPointcut declaredPointcut,
				Pointcut preInstantiationPointcut, MetadataAwareAspectInstanceFactory aspectInstanceFactory) {

			this.declaredPointcut = declaredPointcut;
			this.preInstantiationPointcut = preInstantiationPointcut;
			if (aspectInstanceFactory instanceof LazySingletonAspectInstanceFactoryDecorator) {
				this.aspectInstanceFactory = (LazySingletonAspectInstanceFactoryDecorator) aspectInstanceFactory;
			}
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			// We're either instantiated and matching on declared pointcut,
			// or uninstantiated matching on either pointcut...
			return (isAspectMaterialized() && this.declaredPointcut.matches(method, targetClass)) ||
					this.preInstantiationPointcut.getMethodMatcher().matches(method, targetClass);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, Object... args) {
			// This can match only on declared pointcut.
			return (isAspectMaterialized() && this.declaredPointcut.matches(method, targetClass));
		}

		private boolean isAspectMaterialized() {
			return (this.aspectInstanceFactory == null || this.aspectInstanceFactory.isMaterialized());
		}
	}
}