package com.shengsiyuan.spring.lecture.transction.dao;

import com.shengsiyuan.spring.lecture.transction.domain.Student;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * StudentDAOImpl
 *
 * @author zhucj
 * @since 20210325
 */
public class StudentDAOImpl implements StudentDAO {

	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void saveStudent(Student student) {
		String sql = "insert into student(name, age) values (?, ?)";

		this.jdbcTemplate.update(sql, student.getName(), student.getAge());
	}
}