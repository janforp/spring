package org.springframework.core;

/**
 * Common interface for managing aliases. Serves as a super-interface for
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public interface AliasRegistry {

	/**
	 * Given a name, register an alias for it.
	 *
	 * @param name the canonical name:规范名称
	 * @param alias the alias to be registered:要注册的别名
	 * @throws IllegalStateException if the alias is already in use and may not be overridden - 如果别名已经被使用并且可能不会被覆盖
	 */
	void registerAlias(String name, String alias);

	/**
	 * Remove the specified alias from this registry.
	 *
	 * @param alias the alias to remove
	 * @throws IllegalStateException if no such alias was found
	 */
	void removeAlias(String alias);

	/**
	 * Determine whether the given name is defined as an alias
	 * (as opposed to the name of an actually registered component).
	 *
	 * @param name the name to check
	 * @return whether the given name is an alias
	 */
	boolean isAlias(String name);

	/**
	 * Return the aliases for the given name, if defined.
	 *
	 * @param name the name to check for aliases
	 * @return the aliases, or an empty array if none
	 */
	String[] getAliases(String name);
}