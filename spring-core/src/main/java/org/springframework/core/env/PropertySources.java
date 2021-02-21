package org.springframework.core.env;

import org.springframework.lang.Nullable;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Holder containing one or more {@link PropertySource} objects.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see PropertySource
 * @since 3.1
 */
public interface PropertySources extends Iterable<PropertySource<?>> {

	/**
	 * Return a sequential {@link Stream} containing the property sources.
	 *
	 * @since 5.1
	 */
	default Stream<PropertySource<?>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Return whether a property source with the given name is contained.
	 *
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	boolean contains(String name);

	/**
	 * Return the property source with the given name, {@code null} if not found.
	 *
	 * @param name the {@linkplain PropertySource#getName() name of the property source} to find
	 */
	@Nullable
	PropertySource<?> get(String name);
}