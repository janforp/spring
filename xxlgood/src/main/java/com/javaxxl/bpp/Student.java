package com.javaxxl.bpp;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Student
 *
 * @author zhucj
 * @since 20210225
 */
public class Student {

	private int id;

	private String name;

	@Autowired
	private Teacher teacher;

	public Student() {
	}

	public Student(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public void start() {
		System.out.println("student bean init method [start()] invoked");
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public void setTeacher(Teacher teacher) {
		this.teacher = teacher;
	}
}
