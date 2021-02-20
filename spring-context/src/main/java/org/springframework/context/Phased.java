package org.springframework.context;

/**
 * Interface for objects that may participate in a phased
 * process such as lifecycle management.
 *
 * @author Mark Fisher
 * @see SmartLifecycle
 * @since 3.0
 */
public interface Phased {

	/**
	 * 阶段
	 *
	 * Return the phase value of this object.
	 */
	int getPhase();
}