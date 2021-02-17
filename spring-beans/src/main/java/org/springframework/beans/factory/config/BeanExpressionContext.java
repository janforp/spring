package org.springframework.beans.factory.config;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Context object for evaluating an expression within a bean definition.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class BeanExpressionContext {

	private final ConfigurableBeanFactory beanFactory;

	@Nullable
	private final Scope scope;

	public BeanExpressionContext(ConfigurableBeanFactory beanFactory, @Nullable Scope scope) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.scope = scope;
	}

	public final ConfigurableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	@Nullable
	public final Scope getScope() {
		return this.scope;
	}

	public boolean containsObject(String key) {
		return (this.beanFactory.containsBean(key) ||
				(this.scope != null && this.scope.resolveContextualObject(key) != null));
	}

	@Nullable
	public Object getObject(String key) {
		if (this.beanFactory.containsBean(key)) {
			return this.beanFactory.getBean(key);
		} else if (this.scope != null) {
			return this.scope.resolveContextualObject(key);
		} else {
			return null;
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BeanExpressionContext)) {
			return false;
		}
		BeanExpressionContext otherContext = (BeanExpressionContext) other;
		return (this.beanFactory == otherContext.beanFactory && this.scope == otherContext.scope);
	}

	@Override
	public int hashCode() {
		return this.beanFactory.hashCode();
	}
}