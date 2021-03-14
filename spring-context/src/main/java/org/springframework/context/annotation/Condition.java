package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A single {@code condition} that must be {@linkplain #matches matched} in order
 * for a component to be registered.
 *
 * <p>Conditions are checked immediately before the bean-definition is due to be
 * registered and are free to veto registration based on any criteria that can
 * be determined at that point.
 *
 * <p>Conditions must follow the same restrictions as {@link BeanFactoryPostProcessor}
 * and take care to never interact with bean instances. For more fine-grained control
 * of conditions that interact with {@code @Configuration} beans consider implementing
 * the {@link ConfigurationCondition} interface.
 *
 * @author Phillip Webb
 * @see ConfigurationCondition
 * @see Conditional
 * @see ConditionContext
 * @since 4.0
 */
@FunctionalInterface
public interface Condition {

	/**
	 * * 使用示例：
	 * *
	 * *     @Conditional(OsxCondition.class)
	 * *     @Bean
	 * *     public Student student(){
	 * *         return new Student();
	 * * 	   }
	 * *
	 * * 	 public class OsxCondition implements Condition {
	 * *
	 * *     @Override
	 * *     public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
	 * *         String property = context.getEnvironment().getProperty("os.name");
	 * *         if(property.equals("Mac OS X")){
	 * *             return true;
	 * *         }
	 * *         return false;
	 * *     }
	 * * 	 }
	 * *
	 * * 	 意思就是说，要实例化 student的条件为，当前的系统必须是 Mac OS X 系统，否则就不会实例化他
	 *
	 * Determine if the condition matches.
	 *
	 * @param context the condition context
	 * @param metadata the metadata of the {@link org.springframework.core.type.AnnotationMetadata class}
	 * or {@link org.springframework.core.type.MethodMetadata method} being checked
	 * @return {@code true} if the condition matches and the component can be registered,
	 * or {@code false} to veto the annotated component's registration
	 */
	boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);
}