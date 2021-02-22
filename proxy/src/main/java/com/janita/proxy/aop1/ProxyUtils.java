//package com.janita.proxy.aop1;
//
//import sun.misc.ProxyGenerator;
//
//import java.io.FileOutputStream;
//import java.io.IOException;
//
///**
// * ProxyUtils
// *
// * @author zhucj
// * @since 20210225
// */
//public class ProxyUtils {
//
//	public static void generateClassFile(Class<?> clazz, String proxyName) {
//		byte[] classFile = ProxyGenerator.generateProxyClass(proxyName, clazz.getInterfaces());
//		FileOutputStream out = null;
//		try {
//			out = new FileOutputStream("/Users/janita/code/sourceCodeLearn/spring-framework-master/proxy/src/main/java/com/janita/proxy/aop1/" + proxyName + ".class");
//			out.write(classFile);
//			out.flush();
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				out.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//}
