package com.shengsiyuan.spring.lecture.aop.service.impl;

import com.shengsiyuan.spring.lecture.aop.service.MyService;

/**
 * MyServiceImpl
 *
 * @author zhucj
 * @since 20210325
 */
public class MyServiceImpl implements MyService {

	@Override
	public void myMethod() {
		System.out.println("myMethod invoke");
	}
}