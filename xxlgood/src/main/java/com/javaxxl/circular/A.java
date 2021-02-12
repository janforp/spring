package com.javaxxl.circular;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * A
 *
 * @author zhucj
 * @since 20210225
 */
public class A {

	private B b;

	@Autowired
	public A(B b) {
		this.b = b;
	}
}