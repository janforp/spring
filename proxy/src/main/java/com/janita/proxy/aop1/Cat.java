package com.janita.proxy.aop1;

/**
 * Cat
 *
 * @author zhucj
 * @since 20210225
 */
public class Cat implements Animal {

	@Override
	public void eat() {
		System.out.println("猫猫吃猫粮！！");
	}
}