package com.javaxxl.lifecycle;

import org.springframework.context.SmartLifecycle;

/**
 * DemoOneLifeCycle
 *
 * @author zhucj
 * @since 20210225
 */
public class DemoTwoLifeCycle implements SmartLifecycle {

	private boolean running = false;

	@Override
	public void start() {
		this.running = true;
		System.out.println("******************* demo two is start !!");
	}

	@Override
	public void stop() {
		this.running = false;
		System.out.println("******************* demo two is stop !!");
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}