package com.javaxxl.beanFactoryBeanPostProcessor;

import java.util.Set;

/**
 * ReplaceObscenitiesBeanFactoryPostProcessor
 *
 * @author zhucj
 * @since 20210225
 */
public class ReplaceObscenitiesBeanFactoryPostProcessor {

	private Set<String> obscenities;

	public Set<String> getObscenities() {
		return obscenities;
	}

	public void setObscenities(Set<String> obscenities) {
		this.obscenities = obscenities;
	}
}
