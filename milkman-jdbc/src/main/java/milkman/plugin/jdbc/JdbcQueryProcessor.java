package milkman.plugin.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import milkman.domain.RequestContainer;
import milkman.domain.ResponseContainer;
import milkman.plugin.jdbc.domain.JdbcRequestContainer;
import milkman.plugin.jdbc.domain.JdbcResponseContainer;
import milkman.plugin.jdbc.domain.JdbcSqlAspect;
import milkman.plugin.jdbc.domain.RowSetResponseAspect;
import milkman.ui.plugin.Templater;

@Slf4j
public class JdbcQueryProcessor extends AbstractJdbcProcessor {

	@SneakyThrows
	public ResponseContainer executeRequest(RequestContainer request, Templater templater) {
		
		if (!(request instanceof JdbcRequestContainer)) {
			throw new IllegalArgumentException("Unsupported request container: " + request.getType());
		}
		
		JdbcSqlAspect sqlAspect = request.getAspect(JdbcSqlAspect.class)
				.orElseThrow(() -> new IllegalArgumentException("Missing Sql Aspect"));
		String finalSql = templater.replaceTags(sqlAspect.getSql());
		
		
		JdbcRequestContainer jdbcRequest = (JdbcRequestContainer)request;
		String jdbcUrl = jdbcRequest.getJdbcUrl();
		log.info("Executing Sql: " + finalSql);

		
		Connection connection = DriverManager.getConnection(templater.replaceTags(jdbcUrl));
		Statement statement = connection.createStatement();
		long startTime = System.currentTimeMillis();
		boolean isResultSet = statement.execute(finalSql);
		long requestTimeInMs = System.currentTimeMillis() - startTime;
		
		JdbcResponseContainer response = new JdbcResponseContainer();
		RowSetResponseAspect rowSetAspect = new RowSetResponseAspect();
		
		if (isResultSet) {
			extractRows(statement.getResultSet(), rowSetAspect);
			response.getStatusInformations().complete(Map.of("Selected Rows", ""+ rowSetAspect.getRows().size()));
		} else {
			response.getStatusInformations().complete(Map.of("Affected Rows", ""+ statement.getUpdateCount()));
		}
		response.getStatusInformations().complete(Map.of("Time", requestTimeInMs + "ms"));

		response.getAspects().add(rowSetAspect);
		
		return response;
	}



}
