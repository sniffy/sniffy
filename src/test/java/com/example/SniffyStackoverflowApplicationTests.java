package com.example;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SniffyStackoverflowApplicationTests
{
	@Autowired
	NamedParameterJdbcTemplate jdbcTemplate;

	@Test
	public void testStackOverflow()
	{
		jdbcTemplate.queryForList("SELECT * FROM DUAL WHERE NULL IN(:ids)",
				new MapSqlParameterSource("ids", IntStream.rangeClosed(0, 1000).boxed().collect(Collectors.toList())));
	}
}
