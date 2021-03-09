package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
@SuppressWarnings("all")
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	public static final String PROFILE_ATTRIBUTE = "profile";

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private XmlReaderContext readerContext;

	/**
	 * 委托
	 */
	@Nullable
	private BeanDefinitionParserDelegate delegate;

	/**
	 * This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 */
	@Override
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		//将上下文 context 保存到 readerContext 字段中
		this.readerContext = readerContext;

		/**
		 * Element getDocumentElement():拿出 document 代表的xml的顶层标签 : <beans> .... </beans>
		 *
		 * <beans xmlns="http://www.springframework.org/schema/beans"
		 * 	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 * 	   xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
		 * 	<bean id="componentA" class="com.javaxxl.ComponentA"/>
		 * 	<bean id="componentB" class="com.javaxxl.ComponentB"/>
		 * </beans>
		 *
		 * 底层真正完成元素的解析，并且注册到工厂
		 * @see DefaultListableBeanFactory#beanDefinitionMap 注册到map
		 */
		doRegisterBeanDefinitions(doc.getDocumentElement());
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		Assert.state(this.readerContext != null, "No XmlReaderContext available");
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor}
	 * to pull the source metadata from the supplied {@link Element}.
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}

	/**
	 * Register each bean definition within the given root {@code <beans/>} element.
	 *
	 * @param root 如下
	 * <beans xmlns="http://www.springframework.org/schema/beans"
	 * ***xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 * ***xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
	 *
	 * *****<bean id="componentA" class="com.javaxxl.ComponentA"/>
	 * *****<bean id="componentB" class="com.javaxxl.ComponentB"/>
	 *
	 * </beans>
	 */
	@SuppressWarnings("deprecation")  // for Environment.acceptsProfiles(String...)
	protected void doRegisterBeanDefinitions(Element root) {
		// Any nested <beans> elements will cause recursion in this method.-- 任何嵌套的<beans>元素都将导致此方法中的递归
		// In order to propagate and preserve <beans> default-* attributes correctly,
		// keep track of the current (parent) delegate, which may be null. Create
		// the new (child) delegate with a reference to the parent for fallback purposes,
		// then ultimately reset this.delegate back to its original (parent) reference.
		// this behavior emulates a stack of delegates without actually necessitating one.
		// 为了正确传播和保留<beans> default- *属性，请跟踪当前（父）委托，该委托可以为null。创建一个新的（子）委托，
		// 并带有对父参考的引用以进行回退，然后最终将this.delegate重设回其原始（父）参考。此行为模拟了一组委托，而实际上没有必要。

		//委托
		BeanDefinitionParserDelegate parent = this.delegate;

		/**
		 * 方法最终返回一个 beans 标签解析器对象
		 * @see org.springframework.beans.factory.xml.BeanDefinitionParserDelegate
		 */
		this.delegate = createDelegate(getReaderContext(), root, parent);

		if (this.delegate.isDefaultNamespace(root)) {
			//一般情况下该条件会成立，并且进入 if 代码块

			/**
			 * 获取beans标签的profile属性
			 * profile可能是：dev/prod/pre/test
			 *
			 * profile = "dev,test;prod"
			 */
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				/**
				 * 如果 beans 标签中有 profile 属性
				 * 将 profile 属性值按照 ,; 拆分成字符串数组
				 */
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				// We cannot use Profiles.of(...) since profile expressions are not supported
				// in XML config. See SPR-12458 for details.
				if (!getReaderContext()
						.getEnvironment()//environment.acceptsProfiles(String[] args):如果返回ture，则说明 beans 标签可以解析成 bd，否则就不解析了
						.acceptsProfiles(specifiedProfiles)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					/**
					 * 说明当前 配置文件 跟当前指定的环境不一致，不能加载
					 */
					return;
				}
			}
		}

		//正常情况会解析该配置文件

		/**
		 * 子类扩展方法
		 */
		preProcessXml(root);
		/**
		 * 真正逻辑
		 * 底层真正完成元素的解析，并且注册到工厂
		 * @see DefaultListableBeanFactory#beanDefinitionMap 注册到map
		 */
		parseBeanDefinitions(root, this.delegate);
		/**
		 * 子类扩展方法
		 */
		postProcessXml(root);

		this.delegate = parent;
	}

	/**
	 * 方法最终返回一个 beans 标签解析器对象
	 *
	 * @param readerContext
	 * @param root
	 * @param parentDelegate
	 * @return
	 */
	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, //上下文
			Element root, //beans标签
			@Nullable BeanDefinitionParserDelegate parentDelegate)//上级
	{
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		delegate.initDefaults(root, parentDelegate);
		return delegate;
	}

	/**
	 * Parse the elements at the root level in the document: "import", "alias", "bean".
	 * -- 在文档的根级别上解析元素：“ import”，“ alias”，“ bean”。
	 *
	 * 底层真正完成元素的解析，并且注册到工厂
	 *
	 * @param root the DOM root element of the document
	 * @param delegate 解析该 dom 是委托给该对象进行的
	 * @see DefaultListableBeanFactory#beanDefinitionMap 注册到map
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {

		/**
		 * 判断root是不是spring默认的命名空间
		 */
		if (delegate.isDefaultNamespace(root)) {

			/**
			 * <beans xmlns="http://www.springframework.org/schema/beans"
			 * 	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			 * 	   xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
			 *
			 * <!-- 根元素 -->
			 * 	<bean id="componentA" class="com.javaxxl.ComponentA"/>
			 * 	<bean id="componentB" class="com.javaxxl.ComponentB"/>
			 *
			 * 	<alias name="componentA" alias="componentA2"/>
			 *
			 * 	<import resource="spring-test.xml"/>
			 * </beans>
			 *
			 * 该 nl 就是 顶层标签 <beans></beans> 里面的子标签 "bean","alias","import"
			 */
			NodeList nl = root.getChildNodes();
			//迭代处理每个子标签
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);

				//排除空格/comment（注释）
				if (node instanceof Element) {
					Element ele = (Element) node;
					if (delegate.isDefaultNamespace(ele)) {
						//说明子标签说明也是 spring 默认标签,真正完成元素的解析
						parseDefaultElement(ele, delegate);
					} else {
						//自定义标签
						delegate.parseCustomElement(ele);
					}
				}
			}
		}

		/**
		 * 不是spring命名空间
		 */
		else {
			//解析自定义标签
			delegate.parseCustomElement(root);
		}
	}

	/**
	 * 真正完成元素的解析，并且注册到工厂
	 * @see DefaultListableBeanFactory#beanDefinitionMap 注册到map
	 *
	 * @param ele <bean></bean>,<alais></alais>,<import></import>等根元素
	 * @param delegate 委托对象
	 */
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			//解析 import 标签
			//<import resource="spring-test.xml"/>
			importBeanDefinitionResource(ele);
		} else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			//解析 alias 标签
			//<alias name="componentA" alias="componentA2"/>
			processAliasRegistration(ele);
		} else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			//解析 bean 标签
			//<bean id="componentB" class="com.javaxxl.ComponentB"/>
			processBeanDefinition(ele, delegate);
		} else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// recurse(递归) -- 嵌套了一个 beans 标签
			//解析 beans 标签
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 */
	protected void importBeanDefinitionResource(Element ele) {
		//resource
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// Resolve system properties: e.g. "${user.dir}"
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<>(4);

		// Discover whether the location is an absolute or relative URI
		boolean absoluteLocation = false;
		try {
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		} catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		if (absoluteLocation) {
			try {
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		} else {
			// No URL -> considering resource location as relative to the current file.
			try {
				int importCount;
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				} else {
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			} catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from relative location [" + location + "]", ele, ex);
			}
		}
		Resource[] actResArray = actualResources.toArray(new Resource[0]);
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * Process the given alias element, registering the alias with the registry.
	 *
	 * <alias name = "a" alias = "b" />
	 *
	 * 表示 beanName 为 a 的 beab，给他一个 alias 为 b
	 */
	protected void processAliasRegistration(Element ele) {
		//name
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		//alias
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				getReaderContext().getRegistry().registerAlias(name, alias);
			} catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * Process the given bean element, parsing the bean definition
	 * -- 第一步：处理给定的bean元素，解析bean定义
	 *
	 * and registering it with the registry.
	 * -- 第二步：把bd注册到registry
	 *
	 * @param ele <bean id="componentB" class="com.javaxxl.ComponentB"/>
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		//通过该方法就把一个<bean>标签解析成一个 bd

		/**
		 * 解析 bean 标签,<bean> 标签中所有的属性，配置都已经解析出来，
		 * 并已经放到 {@link BeanDefinitionHolder#beanDefinition} 中了
		 * 解析完成之后返回一个 bdHolder，主要保存了别名信息以及bd以及别名
		 *
		 * 一个BeanDefinitionHolder 其实就维护了一个 <bean></bean>的完整定义
		 */
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			/**
			 * 如果该 bd 需要被装饰，则使用该方法处理，主要处理自定义属性
			 */
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				/**
				 * Register the final decorated instance.注册bd
				 * 将 bd 注册到容器中 注册中心实例：DefaultListableBeanFactory
				 * @see DefaultListableBeanFactory#beanDefinitionMap 注册到这个map中
				 */
				BeanDefinitionReaderUtils.registerBeanDefinition(

						bdHolder,

						//注册表，其实就是工厂
						getReaderContext().getRegistry());

				//上面代码执行之后，bd已经注册到register中了
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// Send registration event.触发bd注册完成的事件，观察者模式
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}
}