package com.example.cinemabooking.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Fail closed if schema migration failed: booking must never run without its database guard. */
@Component
public class SeatConstraintVerifier implements ApplicationRunner {
    private final DataSource dataSource;
    public SeatConstraintVerifier(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            boolean generated = false;
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, "tickets", "active_seat_number")) {
                if (columns.next()) generated = "YES".equalsIgnoreCase(columns.getString("IS_GENERATEDCOLUMN"));
            }
            Set<String> indexedColumns = new HashSet<>();
            try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, "tickets", true, false)) {
                while (indexes.next()) {
                    String indexName = indexes.getString("INDEX_NAME");
                    if (indexName != null && (indexName.equalsIgnoreCase("uk_movie_active_seat")
                            || indexName.toLowerCase(java.util.Locale.ROOT).startsWith("uk_movie_active_seat_index_"))) {
                        String column = indexes.getString("COLUMN_NAME");
                        if (column != null) indexedColumns.add(column.toLowerCase(java.util.Locale.ROOT));
                    }
                }
            }
            if (!generated || !indexedColumns.equals(Set.of("movie_id", "active_seat_number"))) {
                throw new IllegalStateException("محدودیت یکتای صندلی فعال در دیتابیس وجود ندارد؛ برنامه برای جلوگیری از فروش تکراری اجرا نمی‌شود");
            }
        }
    }
}
