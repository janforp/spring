package org.springframework.context;

import java.util.EventObject;

/**
 * Class to be extended by all application events. Abstract as it doesn't make sense for generic events to be published directly.
 * -- 由所有应用程序事件扩展的类。因为直接发布一般事件没有意义，所以定义成一个抽象类。
 *
 * 如果要想发布自己的事件，则需要继承该类，定义自己的事件源
 * 然后在适当的时机发布时机即可
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.event.EventListener
 */
public abstract class ApplicationEvent extends EventObject {

	/**
	 * use serialVersionUID from Spring 1.2 for interoperability.
	 */
	private static final long serialVersionUID = 7099057708183571937L;

	/**
	 * System time when the event happened.
	 */
	private final long timestamp;

	/**
	 * Create a new {@code ApplicationEvent}.
	 *
	 * @param source the object on which the event initially occurred or with
	 * which the event is associated (never {@code null})
	 */
	public ApplicationEvent(Object source) {
		/**
		 * 事件源
		 */
		super(source);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Return the system time in milliseconds when the event occurred.
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}
}