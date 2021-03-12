package com.shengsiyuan.spring.lecture.staticproxy;

/**
 * ProxyTarget
 *
 * @author zhucj
 * @since 20210325
 */
public class StaticProxyTarget implements Target {

	private Target realTarget;

	public StaticProxyTarget(Target realTarget) {
		this.realTarget = realTarget;
	}

	public StaticProxyTarget() {
		if (realTarget == null) {
			realTarget = new RealTarget();
		}
	}

	public void setRealTarget(Target realTarget) {
		this.realTarget = realTarget;
	}

	@Override
	public void myRequest() {
		beforeRequest();
		realTarget.myRequest();
		afterRequest();
	}

	private void beforeRequest() {

		System.out.println("请求之前");
	}

	private void afterRequest() {

		System.out.println("请求之后");
	}
}