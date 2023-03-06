package org.springframework.debug.resolveBeforeInstantiation;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class MyMethodInterceptor implements MethodInterceptor {

	@Override
	public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
		System.out.println("执行目标方法之前："+method);
		Object o1 = methodProxy.invokeSuper(o, objects);
		System.out.println("执行目标方法之后："+method);
		return o1;
	}
}
