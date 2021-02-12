package org.springframework.core;

import org.springframework.util.Assert;

/**
 * {@link ThreadLocal} subclass that exposes a specified name
 * as {@link #toString()} result (allowing for introspection).
 *
 * @param <T> the value type
 * @author Juergen Hoeller
 * @see NamedInheritableThreadLocal
 * @since 2.5.2
 */
public class NamedThreadLocal<T> extends ThreadLocal<T> {

	/**
	 * 扩展该类的目的就是添加一个名称，然后复写toString方法
	 */
	private final String name;

	/**
	 * Create a new NamedThreadLocal with the given name.
	 *
	 * @param name a descriptive name for this ThreadLocal
	 */
	public NamedThreadLocal(String name) {
		Assert.hasText(name, "Name must not be empty");
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}
}