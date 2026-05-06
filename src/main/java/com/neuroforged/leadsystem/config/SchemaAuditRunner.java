package com.neuroforged.leadsystem.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class SchemaAuditRunner implements ApplicationRunner {

    private final EntityManagerFactory emf;
    private final JdbcTemplate jdbcTemplate;

    @Value("${neuroforged.schema-audit.fail-on-drift:false}")
    private boolean failOnDrift;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("SchemaAuditRunner: checking entity/DB column alignment...");

        SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);
        var metamodel = sf.getMappingMetamodel();

        List<String> driftReport = new ArrayList<>();

        metamodel.forEachEntityDescriptor(entityDescriptor -> {
            if (!(entityDescriptor instanceof AbstractEntityPersister persister)) return;

            String table = persister.getTableName();
            // Strip schema prefix if present (e.g. "public.lead" → "lead")
            String simpleTable = table.contains(".") ? table.substring(table.lastIndexOf('.') + 1) : table;

            Set<String> dbColumns = fetchDbColumns(simpleTable);
            if (dbColumns.isEmpty()) {
                log.warn("SchemaAuditRunner: table '{}' not found in information_schema — entity {} may be unmapped", simpleTable, persister.getEntityName());
                driftReport.add("MISSING TABLE: " + simpleTable);
                return;
            }

            String[] mappedColumns = persister.getPropertyColumnNames(persister.getIdentifierPropertyName() != null
                    ? persister.getIdentifierPropertyName() : "id");

            // Gather all mapped column names across all properties
            List<String> entityColumns = new ArrayList<>();
            for (int i = 0; i < persister.getPropertyNames().length; i++) {
                String[] cols = persister.getPropertyColumnNames(i);
                for (String col : cols) {
                    if (col != null && !col.isBlank()) {
                        entityColumns.add(col.toLowerCase());
                    }
                }
            }
            // Also include the identifier column(s)
            for (String idCol : persister.getIdentifierColumnNames()) {
                if (idCol != null && !idCol.isBlank()) {
                    entityColumns.add(idCol.toLowerCase());
                }
            }

            for (String entityCol : entityColumns) {
                if (!dbColumns.contains(entityCol.toLowerCase())) {
                    String msg = "table=" + simpleTable + ", column=" + entityCol + " (in entity, missing in DB)";
                    log.warn("SchemaAuditRunner: DRIFT DETECTED — {}", msg);
                    driftReport.add(msg);
                }
            }
        });

        if (driftReport.isEmpty()) {
            log.info("SchemaAuditRunner: no schema drift detected.");
        } else {
            log.warn("SchemaAuditRunner: {} drift(s) found: {}", driftReport.size(), driftReport);
            if (failOnDrift) {
                throw new IllegalStateException("Schema drift detected — startup aborted. Set neuroforged.schema-audit.fail-on-drift=false to warn only. Drifts: " + driftReport);
            }
        }
    }

    private Set<String> fetchDbColumns(String tableName) {
        List<String> cols = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = ? AND table_schema = current_schema()",
                String.class,
                tableName
        );
        return cols.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }
}
