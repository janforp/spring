package com.javaxxl.propertyeditor;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 自定义属性编辑器
 *
 * @author zhucj
 * @see Student#setBirthday(java.util.Date)
 * @since 20210225
 */
public class DatePropertyEditorRegistrar implements PropertyEditorRegistrar {

	@Override
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		System.out.println(registry);
		registry.registerCustomEditor(Date.class, new DatePropertyEditor());
	}

	private static class DatePropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			System.out.println(text);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				Date date = sdf.parse(text);
				this.setValue(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
}
