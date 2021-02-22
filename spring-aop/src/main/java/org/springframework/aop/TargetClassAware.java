package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * Minimal interface for exposing the target class behind a proxy.
 *
 * <p>Implemented by AOP proxy objects and proxy factories
 * (via {@link org.springframework.aop.framework.Advised})
 * as well as by {@link TargetSource TargetSources}.
 *
 * @author Juergen Hoeller
 * @see org.springframework.aop.support.AopUtils#getTargetClass(Object)
 * @since 2.0.3
 */
public interface TargetClassAware {

	/**
	 * Return the target class behind the implementing object
	 * (typically a proxy configuration or an actual proxy).
	 *
	 * @return the target Class, or {@code null} if not known
	 */
	@Nullable
	Class<?> getTargetClass();
}