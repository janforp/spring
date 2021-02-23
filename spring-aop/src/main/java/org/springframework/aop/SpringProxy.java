package org.springframework.aop;

/**
 * Marker interface（标记接口） implemented by all AOP proxies.-- 由所有AOP代理实现的标记接口。
 *
 * Used to detect whether or not objects are Spring-generated proxies. -- 用于检测对象是否是Spring生成的代理。
 *
 * @author Rob Harrop
 * @see org.springframework.aop.support.AopUtils#isAopProxy(Object)
 * @since 2.0.1
 */
public interface SpringProxy {

}