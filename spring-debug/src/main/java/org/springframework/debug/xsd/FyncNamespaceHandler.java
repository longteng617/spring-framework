package org.springframework.debug.xsd;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @Author YangQinglong
 * @Date 2022/6/27 4:36 PM
 */
public class FyncNamespaceHandler extends NamespaceHandlerSupport {


	@Override
	public void init() {
		registerBeanDefinitionParser("user",new UserBeanDefinitionParser());
	}
}
