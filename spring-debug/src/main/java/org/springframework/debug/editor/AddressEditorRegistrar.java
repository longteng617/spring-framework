package org.springframework.debug.editor;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

/**
 * @Author YangQinglong
 * @Date 2022/6/28 6:00 PM
 */
public class AddressEditorRegistrar implements PropertyEditorRegistrar {

	@Override
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(Address.class,new AddressEditor());
	}
}
