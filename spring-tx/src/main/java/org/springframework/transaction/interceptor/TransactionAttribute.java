package org.springframework.transaction.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;

import java.util.Collection;

/**
 * This interface adds a {@code rollbackOn} specification to {@link TransactionDefinition}.
 * As custom {@code rollbackOn} is only possible with AOP, it resides in the AOP-related
 * transaction subpackage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @see DefaultTransactionAttribute
 * @see RuleBasedTransactionAttribute
 * @since 16.03.2003
 */
public interface TransactionAttribute extends TransactionDefinition {

	/**
	 * Return a qualifier value associated with this transaction attribute.
	 * <p>This may be used for choosing a corresponding transaction manager
	 * to process this specific transaction.
	 *
	 * @since 3.0
	 */
	@Nullable
	String getQualifier();

	/**
	 * Return labels associated with this transaction attribute.
	 * <p>This may be used for applying specific transactional behavior
	 * or follow a purely descriptive nature.
	 *
	 * @since 5.3
	 */
	Collection<String> getLabels();

	/**
	 * Should we roll back on the given exception?
	 * -- 我们应该回退给定的异常吗
	 *
	 * @param ex the exception to evaluate
	 * @return whether to perform a rollback or not
	 * @see DefaultTransactionAttribute#rollbackOn(java.lang.Throwable) 默认是任何异常都回滚
	 */
	boolean rollbackOn(Throwable ex);
}