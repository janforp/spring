package com.javaxxl.propertyeditor;

import java.util.Date;

/**
 * Student
 *
 * @author zhucj
 * @since 20210225
 */
public class Student {

	private String name;

	/**
	 * @see DatePropertyEditorRegistrar
	 */
	private Date birthday;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}
}
