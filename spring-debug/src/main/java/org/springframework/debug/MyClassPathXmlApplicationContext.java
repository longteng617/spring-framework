package org.springframework.debug;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @Author YangQinglong
 * @Date 2022/6/27 3:57 PM
 */
public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {

	public MyClassPathXmlApplicationContext(String configLocation){
		super(configLocation);
	}

	@Override
	protected void initPropertySources() {
		System.out.println("扩展initPropertySource");
		// 这里添加了一个name属性到Environment里面，以方便我们在后面用到
		getEnvironment().getSystemProperties().put("name","long");
		// 这里要求Environment中必须包含username属性，如果不包含，则抛出异常
		getEnvironment().setRequiredProperties("requiredField");
	}

	@Override
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		beanFactory.setAllowBeanDefinitionOverriding(false);
		beanFactory.setAllowCircularReferences(false);
		super.customizeBeanFactory(beanFactory);
	}
}

