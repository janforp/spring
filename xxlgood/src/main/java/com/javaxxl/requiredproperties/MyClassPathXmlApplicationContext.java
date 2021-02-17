package com.javaxxl.requiredproperties;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * MyClassPathXmlApplicationContext
 *
 * @author zhucj
 * @since 20210225
 */
public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {

	public MyClassPathXmlApplicationContext(String... configLocations) {
		super(configLocations);
	}

	@Override
	protected void initPropertySources() {
		//LG_HOME 配置必须要有
		/**
		 * @see AbstractApplicationContext#prepareRefresh()
		 */
		getEnvironment().setRequiredProperties("LG_HOME");
	}
}
