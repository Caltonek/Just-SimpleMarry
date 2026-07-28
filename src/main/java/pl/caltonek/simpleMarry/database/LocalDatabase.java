package pl.caltonek.simpleMarry.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pl.caltonek.simpleMarry.model.Marriage;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public final class LocalDatabase implements AutoCloseable {

    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS marriages (player_one VARCHAR(36) PRIMARY KEY, player_two VARCHAR(36) NOT NULL);";
    private static final String SELECT_ALL_SQL = "SELECT player_one, player_two FROM marriages";
    private static final String DELETE_SQL = "DELETE FROM marriages WHERE player_one = ? OR player_two = ?";
    private static final String MERGE_SQL = "MERGE INTO marriages (player_one, player_two) KEY(player_one) VALUES (?, ?)";

    private final File dataFolder;
    private final Logger logger;
    private HikariDataSource dataSource;

    public LocalDatabase(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public void init() throws SQLException {
        File databaseDir = new File(dataFolder, "database");
        if (!databaseDir.exists() && !databaseDir.mkdirs()) {
            throw new IllegalStateException("Failed to create db directory: " + databaseDir.getAbsolutePath());
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:" + new File(databaseDir, "marriages").getAbsolutePath() + ";DB_CLOSE_DELAY=-1");
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setPoolName("SimpleMarry-H2-Pool");

        this.dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE_SQL);
        }
    }

    public Set<Marriage> loadAll() throws SQLException {
        Set<Marriage> result = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                result.add(new Marriage(
                        UUID.fromString(resultSet.getString(1)),
                        UUID.fromString(resultSet.getString(2))
                ));
            }
        }
        return result;
    }

    public void saveBatch(Collection<Marriage> toAdd, Collection<UUID> toDelete) throws SQLException {
        if (toAdd.isEmpty() && toDelete.isEmpty()) return;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!toDelete.isEmpty()) {
                    try (PreparedStatement deleteStmt = connection.prepareStatement(DELETE_SQL)) {
                        for (UUID uuid : toDelete) {
                            String id = uuid.toString();
                            deleteStmt.setString(1, id);
                            deleteStmt.setString(2, id);
                            deleteStmt.addBatch();
                        }
                        deleteStmt.executeBatch();
                    }
                }

                if (!toAdd.isEmpty()) {
                    try (PreparedStatement mergeStmt = connection.prepareStatement(MERGE_SQL)) {
                        for (Marriage marriage : toAdd) {
                            mergeStmt.setString(1, marriage.player1().toString());
                            mergeStmt.setString(2, marriage.player2().toString());
                            mergeStmt.addBatch();
                        }
                        mergeStmt.executeBatch();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("H2 connection pool closed.");
        }
    }
}