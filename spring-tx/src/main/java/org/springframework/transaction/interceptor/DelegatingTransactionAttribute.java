package org.springframework.transaction.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.DelegatingTransactionDefinition;

import java.io.Serializable;
import java.util.Collection;

/**
 * {@link TransactionAttribute} implementation that delegates all calls to a given target
 * {@link TransactionAttribute} instance. Abstract because it is meant to be subclassed,
 * with subclasses overriding specific methods that are not supposed to simply delegate
 * to the target instance.
 *
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @since 1.2
 */
@SuppressWarnings("serial")
public abstract class DelegatingTransactionAttribute

		extends DelegatingTransactionDefinition

		implements TransactionAttribute, Serializable {

	/**
	 * 委托者
	 */
	private final TransactionAttribute targetAttribute;

	/**
	 * Create a DelegatingTransactionAttribute for the given target attribute.
	 *
	 * @param targetAttribute the target TransactionAttribute to delegate to
	 */
	public DelegatingTransactionAttribute(TransactionAttribute targetAttribute) {
		super(targetAttribute);
		this.targetAttribute = targetAttribute;
	}

	@Override
	@Nullable
	public String getQualifier() {
		return this.targetAttribute.getQualifier();
	}

	@Override
	public Collection<String> getLabels() {
		return this.targetAttribute.getLabels();
	}

	@Override
	public boolean rollbackOn(Throwable ex) {
		return this.targetAttribute.rollbackOn(ex);
	}
}