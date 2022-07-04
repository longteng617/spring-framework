package org.springframework.debug.editor;

import java.beans.PropertyEditorSupport;

/**
 * @Author YangQinglong
 * @Date 2022/6/28 5:58 PM
 */
public class AddressEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String[] s = text.split("_");
		Address address = new Address();
		address.setProvince(s[0]);
		address.setCity(s[1]);
		address.setArea(s[2]);
		this.setValue(address);
	}
}
