package io.sniffy.reporter;

import io.sniffy.CurrentThreadSpy;
import io.sniffy.servlet.RequestStats;
import io.sniffy.sql.SqlStatement;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InfluxDbReporter implements MetricsReporter {

    private final String url;
    private final String username;
    private final String password;

    private final String database;

    private InfluxDB influxDB;

    public InfluxDbReporter(String url) {
        this(url, "sniffy");
    }

    public InfluxDbReporter(String url, String database) {
        this(url, database, null, null);
    }

    public InfluxDbReporter(String url, String database, String username, String password) {
        this.url = url;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    public void init() {

        try {

            if (null == username || "".equals(username) || null == password || "".equals(password)) {
                influxDB = InfluxDBFactory.connect(url);
            } else {
                influxDB = InfluxDBFactory.connect(url, username, password);
            }

            if (!influxDB.databaseExists(database)) {
                influxDB.createDatabase(database);
            }

            influxDB.setDatabase(database);

            influxDB.enableBatch(1000, 1, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
            // TODO: log me maybe?
        }

    }

    @Override
    public void report(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            String requestId,
            CurrentThreadSpy spy,
            RequestStats requestStats) {

        if (null == influxDB) return;

        String normalizedUrl = null;

        Object bestMatchingPattern = httpServletRequest.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");
        if (null != bestMatchingPattern && bestMatchingPattern instanceof String) {

            String bmp = String.class.cast(bestMatchingPattern);

            StringBuilder sb = new StringBuilder();

            String contextPath = httpServletRequest.getContextPath();
            sb.append(contextPath);

            String servletPath = httpServletRequest.getServletPath();

            if (!bmp.startsWith(servletPath)) {
                sb.append(servletPath);
            }

            sb.append(bmp);

            normalizedUrl = sb.toString();
        }

        String exactUrl = httpServletRequest.getRequestURI();

        if (null == normalizedUrl) {
            normalizedUrl = exactUrl;
        }

        int sqlQueries = 0;
        long sqlBytesDown = 0;
        long sqlBytesUp = 0;
        long sqlTime = 0;

        Map<StatementMetaData, SqlStats> executedStatements = requestStats.getExecutedStatements();
        if (null != executedStatements) {
            for (Map.Entry<StatementMetaData, SqlStats> entry : executedStatements.entrySet()) {
                SqlStats sqlStats = entry.getValue();
                sqlBytesDown += sqlStats.bytesDown.longValue();
                sqlBytesUp += sqlStats.bytesUp.longValue();
                if (entry.getKey().query != SqlStatement.SYSTEM) {
                    sqlQueries += sqlStats.queries.intValue();
                }
                sqlTime += sqlStats.elapsedTime.longValue();
                influxDB.write(
                        Point.measurement("sql").
                                tag("url", normalizedUrl).
                                tag("query", entry.getKey().sql).
                                addField("exactUrl", exactUrl).
                                addField("elapsedTime", sqlStats.elapsedTime.longValue()).
                                addField("requestId", requestId).
                                addField("bytesDown", sqlStats.bytesDown.longValue()).
                                addField("bytesUp", sqlStats.bytesUp.longValue()).
                                addField("queries", sqlStats.queries.intValue()).
                                addField("rows", sqlStats.rows.intValue()).
                                build()
                );
            }
        }

        influxDB.write(
                Point.measurement("http").
                        tag("url", normalizedUrl).
                        tag("responseCode", Integer.toString(httpServletResponse.getStatus())).
                        tag("result", Integer.toString(httpServletResponse.getStatus() / 300)).
                        addField("exactUrl", exactUrl).
                        addField("requestId", requestId).
                        addField("elapsedTime", requestStats.getElapsedTime()).
                        addField("timeToFirstByte", requestStats.getTimeToFirstByte()).
                        addField("sqlQueries", sqlQueries).
                        addField("sqlBytesDown", sqlBytesDown).
                        addField("sqlBytesUp", sqlBytesUp).
                        addField("sqlTime", sqlTime).
                        build()
        );

    }

}
