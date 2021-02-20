package com.javaxxl.lifecycle;

import org.springframework.context.Lifecycle;

/**
 * DemoOneLifeCycle
 *
 * @author zhucj
 * @since 20210225
 */
public class DemoOneLifeCycle implements Lifecycle {

	private boolean running = false;

	@Override
	public void start() {
		this.running = true;
		System.out.println("************* demo one is start !");
	}

	@Override
	public void stop() {
		this.running = false;
		System.out.println("************* demo one is stop !");
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}