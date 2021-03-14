package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a component is only eligible for registration when all
 * {@linkplain #value specified conditions} match.
 *
 * <p>A <em>condition</em> is any state that can be determined programmatically
 * before the bean definition is due to be registered (see {@link Condition} for details).
 *
 * <p>The {@code @Conditional} annotation may be used in any of the following ways:
 * <ul>
 * <li>as a type-level annotation on any class directly or indirectly annotated with
 * {@code @Component}, including {@link Configuration @Configuration} classes</li>
 * <li>as a meta-annotation, for the purpose of composing custom stereotype
 * annotations</li>
 * <li>as a method-level annotation on any {@link Bean @Bean} method</li>
 * </ul>
 *
 * <p>If a {@code @Configuration} class is marked with {@code @Conditional},
 * all of the {@code @Bean} methods, {@link Import @Import} annotations, and
 * {@link ComponentScan @ComponentScan} annotations associated with that
 * class will be subject to the conditions.
 *
 * <p><strong>NOTE</strong>: Inheritance of {@code @Conditional} annotations
 * is not supported; any conditions from superclasses or from overridden
 * methods will not be considered. In order to enforce these semantics,
 * {@code @Conditional} itself is not declared as
 * {@link java.lang.annotation.Inherited @Inherited}; furthermore, any
 * custom <em>composed annotation</em> that is meta-annotated with
 * {@code @Conditional} must not be declared as {@code @Inherited}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see Condition
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

	/**
	 * All {@link Condition} classes that must {@linkplain Condition#matches match}
	 * in order for the component to be registered.
	 * -- 必须匹配才能注册组件的所有Condition类
	 *
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
	 */
	Class<? extends Condition>[] value();
}