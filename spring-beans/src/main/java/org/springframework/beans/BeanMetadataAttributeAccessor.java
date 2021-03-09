package org.springframework.beans;

import org.springframework.core.AttributeAccessorSupport;
import org.springframework.lang.Nullable;

/**
 * 该类用来处理保存 bean 标签的 meta 子标签的
 *
 * <bean id="child" class="com.javaxxl.parent.Child">
 * ---<meta key="meta_1" value="val_1"/>
 * ---<meta key="meta_2" value="val_2"/>
 * </bean>
 *
 * Extension of {@link org.springframework.core.AttributeAccessorSupport},
 * holding attributes as {@link BeanMetadataAttribute} objects in order
 * to keep track of the definition source.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class BeanMetadataAttributeAccessor extends AttributeAccessorSupport implements BeanMetadataElement {

	@Nullable
	private Object source;

	/**
	 * Set the configuration source {@code Object} for this metadata element.
	 * <p>The exact type of the object will depend on the configuration mechanism used.
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * * Map with String keys and Object values.
	 * * 每个value就表示一个 <meta key="meta_1" value="val_1"/> 对象的模型： {@link org.springframework.beans.BeanMetadataAttribute} BeanMetadataAttribute
	 * * key:为 key="meta_1" 中的  meta_1
	 * * value:一个 <meta key="meta_1" value="val_1"/> 对象的模型： {@link org.springframework.beans.BeanMetadataAttribute} BeanMetadataAttribute
	 *
	 * Add the given BeanMetadataAttribute to this accessor's set of attributes.
	 *
	 * @param attribute the BeanMetadataAttribute object to register
	 */
	public void addMetadataAttribute(BeanMetadataAttribute attribute) {
		//<meta key="meta_1" value="val_1"/>
		super.setAttribute(attribute.getName(), attribute);
	}

	/**
	 * Look up the given BeanMetadataAttribute in this accessor's set of attributes.
	 *
	 *  * Map with String keys and Object values.
	 *  * 每个value就表示一个 <meta key="meta_1" value="val_1"/> 对象的模型： {@link org.springframework.beans.BeanMetadataAttribute} BeanMetadataAttribute
	 *  * key:为 key="meta_1" 中的  meta_1
	 *  * value:一个 <meta key="meta_1" value="val_1"/> 对象的模型： {@link org.springframework.beans.BeanMetadataAttribute} BeanMetadataAttribute
	 *
	 * @param name the name of the attribute
	 * @return the corresponding BeanMetadataAttribute object,
	 * or {@code null} if no such attribute defined
	 */
	@Nullable
	public BeanMetadataAttribute getMetadataAttribute(String name) {
		return (BeanMetadataAttribute) super.getAttribute(name);
	}

	@Override
	public void setAttribute(String name, @Nullable Object value) {
		super.setAttribute(name, new BeanMetadataAttribute(name, value));
	}

	@Override
	@Nullable
	public Object getAttribute(String name) {
		BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.getAttribute(name);
		return (attribute != null ? attribute.getValue() : null);
	}

	@Override
	@Nullable
	public Object removeAttribute(String name) {
		BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.removeAttribute(name);
		return (attribute != null ? attribute.getValue() : null);
	}
}
