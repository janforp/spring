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
	 * target.method() 【业务方法可能成功也可能失败】
	 *
	 * conn.commit()
	 * or
	 * conn.rollback()
	 */
	default void fuxk() {
		//doF**();
	}
}