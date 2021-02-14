package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.w3c.dom.Document;

/**
 * SPI for parsing an XML document that contains Spring bean definitions. Used by {@link XmlBeanDefinitionReader} for actually parsing a DOM document.
 * -- SPI，用于解析包含Spring bean定义的XML文档。 {@link XmlBeanDefinitionReader}使用它来实际解析DOM文档。
 *
 * <p>
 * Instantiated per document to parse: implementations can hold
 * state in instance variables during the execution of the
 * {@code registerBeanDefinitions} method &mdash; for example, global
 * settings that are defined for all bean definitions in the document.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see XmlBeanDefinitionReader#setDocumentReaderClass
 * @since 18.12.2003
 */
public interface BeanDefinitionDocumentReader {

	/**
	 * Read bean definitions from the given DOM document and register them with the registry in the given reader context.
	 * -- 从给定的DOM文档中读取bean定义，并在给定的 readerContext 中向注册表注册它们。：意思就是 readerContext 中是包含了 注册中心的
	 *
	 * @param doc the DOM document
	 * @param readerContext the current context of the reader
	 * (includes the target registry and the resource being parsed)
	 * @throws BeanDefinitionStoreException in case of parsing errors
	 * @see XmlBeanDefinitionReader#registerBeanDefinitions(org.w3c.dom.Document, org.springframework.core.io.Resource)
	 */
	void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) throws BeanDefinitionStoreException;
}