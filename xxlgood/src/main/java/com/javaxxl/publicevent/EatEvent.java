package com.javaxxl.publicevent;

import org.springframework.context.ApplicationEvent;

/**
 * EatEvent
 *
 * @author zhucj
 * @since 20210225
 */
@SuppressWarnings("serial")
public class EatEvent extends ApplicationEvent {

	private Boolean eatFinished;

	public EatEvent(Boolean eatFinished) {
		super(eatFinished);
		this.eatFinished = eatFinished;
	}

	public void callGirlFriend() {
		System.out.println("********************* 美女,吃完饭了，来收拾一下吧！");
	}

	public void callBrothers() {
		System.out.println("********************* 兄弟们，吃完饭了，来打dota ！");
	}

	public Boolean getEatFinished() {
		return eatFinished;
	}
}
