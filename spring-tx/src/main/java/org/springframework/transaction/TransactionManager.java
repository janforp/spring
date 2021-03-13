package org.springframework.transaction;

/**
 * Marker interface for Spring transaction manager implementations,
 * either traditional or reactive.
 *
 * @author Juergen Hoeller
 * @see PlatformTransactionManager
 * @see ReactiveTransactionManager
 * @since 5.2
 */
public interface TransactionManager {

	/**
	 * 针对数据库事务的操作：
	 * setAutoCommit(false)
	 *
	 * target.method()
	 *
	 * conn.commit()
	 * or
	 * conn.rollback()
	 */
	default void fuxk() {
		//doF**();
	}
}