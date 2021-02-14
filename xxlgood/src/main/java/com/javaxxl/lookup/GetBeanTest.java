package com.javaxxl.lookup;

/**
 * GetBeanTest
 *
 * @author zhucj
 * @since 20210225
 */
public abstract class GetBeanTest {

	public void showMe() {
		this.getBean().showMe();
	}

	public abstract User getBean();
}
