package com.javaxxl.publicevent;

import org.springframework.context.ApplicationListener;

/**
 * EatEventListener
 *
 * @author zhucj
 * @since 20210225
 */
public class EatEventListener implements ApplicationListener<EatEvent> {

	@Override
	public void onApplicationEvent(EatEvent xEvent) {
		if (xEvent.getEatFinished()) {
			xEvent.callGirlFriend();
			System.out.println("xxxx,女朋友拒绝收拾！");
			xEvent.callBrothers();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("满人了，下次带你！");
		}
	}
}