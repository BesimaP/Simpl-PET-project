package persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class ConnectionPool {

    private static volatile ConnectionPool instance = null;
    private static HikariDataSource ds;

    private ConnectionPool() {
    }

    public static ConnectionPool getInstance(String user, String password, String url, String db) {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) {  // Double-checked locking
                    if (System.getenv("DEPLOYED") != null) {
                        ds = createHikariConnectionPool(
                                System.getenv("JDBC_USER"),
                                System.getenv("JDBC_PASSWORD"),
                                System.getenv("JDBC_CONNECTION_STRING"),
                                System.getenv("JDBC_DB"));
                    } else {
                        ds = createHikariConnectionPool(user, password, url, db);
                    }
                    instance = new ConnectionPool();
                }
            }
        }
        return instance;
    }

    private static HikariDataSource createHikariConnectionPool(String user, String password, String url, String db) {
        //   LOGGER.log(Level.INFO, "Initializing Connection Pool for database: {0}", db);

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(String.format(url, db));
        config.setUsername(user);
        config.setPassword(password);

        // Connection Pool Configurations
        config.setMaximumPoolSize(10); // Default is 10
        config.setMinimumIdle(2);      // Ensures some connections are always available
        config.setIdleTimeout(30000);  // 30 seconds idle timeout
        config.setConnectionTimeout(30000); // Max wait time for a connection
        config.setPoolName("Postgresql-Pool");

        // Optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        if (ds == null) {
            throw new SQLException("Connection pool is not initialized.");
        }

        return ds.getConnection();
    }

    public static void close() {
        if (ds != null) {
            ds.close();
            ds = null;
        }
    }
}