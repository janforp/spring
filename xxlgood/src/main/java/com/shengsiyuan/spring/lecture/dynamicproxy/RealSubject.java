package com.shengsiyuan.spring.lecture.dynamicproxy;

/**
 * RealSubject
 *
 * @author zhucj
 * @since 20210325
 */
public class RealSubject implements Subject {

	@Override
	public void myRequest() {
		System.out.println("调用真实对象的方法");
	}
}
