package com.shengsiyuan.spring.lecture.staticproxy;

/**
 * RealTarget
 *
 * @author zhucj
 * @since 20210325
 */
public class RealTarget implements Target {

	@Override
	public void myRequest() {
		System.out.println("From Target Object");
	}
}