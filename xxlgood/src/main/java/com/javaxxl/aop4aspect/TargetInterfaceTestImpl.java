package com.javaxxl.aop4aspect;

/**
 * TargetInterfaceImpl
 *
 * @author zhucj
 * @since 20210225
 */
public class TargetInterfaceTestImpl implements TargetInterfaceTest {

	@Override
	public void doSomeTest() {
		System.out.println("doSomeTest 执行");
	}

	@Override
	public void doOtherTest() {
		System.out.println("doOtherTest 执行");
	}

	@Override
	public void a() {
		System.out.println("a 执行");

	}

	@Override
	public void b() {
		System.out.println("b 执行");
	}
}
