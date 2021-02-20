package org.springframework.context;

import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Interface that encapsulates event publication functionality.
 *
 * <p>Serves as a super-interface for {@link ApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see ApplicationContext
 * @see ApplicationEventPublisherAware
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.event.EventPublicationInterceptor
 * @since 1.1.1
 */
@FunctionalInterface
public interface ApplicationEventPublisher {

	/**
	 * Notify all <strong>matching</strong> listeners registered with this
	 * application of an application event. Events may be framework events
	 * (such as ContextRefreshedEvent) or application-specific events.
	 * <p>Such an event publication step is effectively a hand-off to the
	 * multicaster and does not imply synchronous/asynchronous execution
	 * or even immediate execution at all. Event listeners are encouraged
	 * to be as efficient as possible, individually using asynchronous
	 * execution for longer-running and potentially blocking operations.
	 *
	 * @param event the event to publish
	 * @see #publishEvent(Object)
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see AbstractApplicationContext#publishEvent(org.springframework.context.ApplicationEvent) 发布函数
	 * @see AbstractApplicationContext#publishEvent(java.lang.Object, org.springframework.core.ResolvableType) 逻辑
	 * @see SimpleApplicationEventMulticaster#multicastEvent 事件发布逻辑
	 */
	default void publishEvent(ApplicationEvent event) {
		publishEvent((Object) event);
	}

	/**
	 * Notify all <strong>matching</strong> listeners registered with this
	 * application of an event.
	 * <p>If the specified {@code event} is not an {@link ApplicationEvent},
	 * it is wrapped in a {@link PayloadApplicationEvent}.
	 * <p>Such an event publication step is effectively a hand-off to the
	 * multicaster and does not imply synchronous/asynchronous execution
	 * or even immediate execution at all. Event listeners are encouraged
	 * to be as efficient as possible, individually using asynchronous
	 * execution for longer-running and potentially blocking operations.
	 *
	 * @param event the event to publish
	 * @see #publishEvent(ApplicationEvent)
	 * @see PayloadApplicationEvent
	 * @since 4.2
	 */
	void publishEvent(Object event);
}