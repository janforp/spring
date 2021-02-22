package com.javaxxl.classutils;

import org.springframework.util.ClassUtils;

import java.util.Set;

/**
 * Main
 *
 * @author zhucj
 * @since 20210225
 */
public class Main {

	public static void main(String[] args) {
		Set<Class<?>> classSet = ClassUtils.getAllInterfacesForClassAsSet(A.class, null);
	}
}
