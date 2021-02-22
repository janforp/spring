package com.javaxxl.aop0;

/**
 * Cat
 *
 * @author zhucj
 * @since 20210225
 */
public class Cat implements Animal {

	@Override
	public void eat() {
		System.out.println("*************** 猫猫吃猫粮！！");
	}

	@Override
	public void go() {
		System.out.println("*************** 猫猫跑！！");
	}
}