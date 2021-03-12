package com.shengsiyuan.spring.lecture.staticproxy;

/**
 * 静态代理！
 *
 * @author zhucj
 * @since 20210325
 */
public class StaticProxyClient {

	public static void main(String[] args) {
		Target target = new StaticProxyTarget();
		target.myRequest();
	}
}