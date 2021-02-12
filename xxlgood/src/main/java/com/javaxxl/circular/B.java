package com.javaxxl.circular;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * B
 *
 * @author zhucj
 * @since 20210225
 */
public class B {

	private A a;

	@Autowired
	public B(A a) {
		this.a = a;
	}
}
