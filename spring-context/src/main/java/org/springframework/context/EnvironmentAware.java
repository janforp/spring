package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.env.Environment;

/**
 * Interface to be implemented by any bean that wishes to be notified
 * of the {@link Environment} that it runs in.
 *
 * @author Chris Beams
 * @see org.springframework.core.env.EnvironmentCapable
 * @since 3.1
 */
public interface EnvironmentAware extends Aware {

	/**
	 * Set the {@code Environment} that this component runs in.
	 */
	void setEnvironment(Environment environment);
}