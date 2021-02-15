package org.springframework.beans.factory.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

/**
 * Internal representation of a null bean instance, e.g. for a {@code null} value
 * returned from {@link FactoryBean#getObject()} or from a factory method.
 *
 * <p>Each such null bean is represented by a dedicated {@code NullBean} instance
 * which are not equal to each other, uniquely differentiating each bean as returned
 * from all variants of {@link org.springframework.beans.factory.BeanFactory#getBean}.
 * However, each such instance will return {@code true} for {@code #equals(null)}
 * and returns "null" from {@code #toString()}, which is how they can be tested
 * externally (since this class itself is not public).
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
final class NullBean {

	NullBean() {
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || obj == null);
	}

	@Override
	public int hashCode() {
		return NullBean.class.hashCode();
	}

	@Override
	public String toString() {
		return "null";
	}
}