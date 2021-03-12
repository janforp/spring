package com.shengsiyuan.spring.lecture.transction.service;

import com.shengsiyuan.spring.lecture.transction.dao.StudentDAO;
import com.shengsiyuan.spring.lecture.transction.domain.Student;

/**
 * StudentServiceImpl
 *
 * @author zhucj
 * @since 20210325
 */
public class StudentServiceImpl implements StudentService {

	private StudentDAO studentDAO;

	public void setStudentDAO(StudentDAO studentDAO) {
		this.studentDAO = studentDAO;
	}

	@Override
	public void saveStudent(Student student) {
		studentDAO.saveStudent(student);
	}
}
