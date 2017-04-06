package io.sniffy;

import io.sniffy.sql.SniffyDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.qatools.allure.annotations.Features;

import javax.sql.DataSource;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SniffyStackoverflowApplicationTests extends BaseTest
{
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Before
	public void setupJdbcTemplate() {
		JdbcDataSource h2DataSource = new JdbcDataSource();
		h2DataSource.setURL("jdbc:h2:mem:");

		DataSource wrap = SniffyDataSource.wrap(h2DataSource);

		jdbcTemplate = new NamedParameterJdbcTemplate(wrap);
	}

	@Test
	@Features("issues/331")
	public void testStackOverflow()
	{
		jdbcTemplate.queryForList("SELECT * FROM DUAL WHERE NULL IN(:ids)",
				new MapSqlParameterSource("ids", IntStream.rangeClosed(0, 1000).boxed().collect(Collectors.toList())));
	}
}
