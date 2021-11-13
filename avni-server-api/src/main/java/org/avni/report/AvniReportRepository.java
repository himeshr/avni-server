package org.avni.report;

import org.avni.application.FormMapping;
import org.avni.domain.Concept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AvniReportRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ReportHelper reportHelper;

    @Autowired
    public AvniReportRepository(NamedParameterJdbcTemplate jdbcTemplate,
                                ReportHelper reportHelper) {
        this.jdbcTemplate = jdbcTemplate;
        this.reportHelper = reportHelper;
    }

    public List<AggregateReportResult> generateAggregatesForCodedConcept(Concept concept, FormMapping formMapping) {
        String query = "with base_result as (\n" +
                "    select unnest(case\n" +
                "                      when jsonb_typeof(${obsColumn} -> ${conceptUUID}) = 'array'\n" +
                "                          then TRANSLATE((${obsColumn} -> ${conceptUUID})::jsonb::text, '[]', '{}')::TEXT[]\n" +
                "                      else ARRAY [${obsColumn} ->> ${conceptUUID}] end) as indicator,\n" +
                "           count(*)                                                     as count\n" +
                "    from ${dynamicFrom}\n" +
                "    where ${dynamicWhere}\n" +
                "    group by 1\n" +
                ")\n" +
                "select coalesce(concept_name(indicator), coalesce(indicator, 'Not answered')) indicator,\n" +
                "       count\n" +
                "from base_result";
        String queryWithConceptUUID = query.replace("${conceptUUID}", "'" + concept.getUuid() + "'");
        return jdbcTemplate.query(reportHelper.buildQuery(formMapping, queryWithConceptUUID), new AggregateReportMapper());
    }

    public List<AggregateReportResult> generateAggregatesForEntityByType(String entity, String operationalType, String operationalTypeIdColumn, String dynamicWhere) {
        String baseQuery = "select o.name as indicator,\n" +
                "       count(*) as count\n" +
                "from ${entity} e\n" +
                "         join ${operational_type} o on e.${operational_type_id} = o.${operational_type_id}\n" +
                "where e.is_voided = false\n" +
                "  and o.is_voided = false\n" +
                "  ${dynamic_where}\n" +
                "group by o.name";
        String query = baseQuery
                .replace("${entity}", entity)
                .replace("${operational_type}", operationalType)
                .replace("${operational_type_id}", operationalTypeIdColumn)
                .replace("${dynamic_where}", dynamicWhere);
        return jdbcTemplate.query(query, new AggregateReportMapper());
    }

    public List<CountForDay> generateDayWiseActivities() {
        String query = "select date for_date, sum(count) activity_count\n" +
                "from (select registration_date date, count(*) count\n" +
                "      from individual\n" +
                "      group by registration_date\n" +
                "      union all\n" +
                "      select date(encounter_date_time) date, count(*) count\n" +
                "      from encounter\n" +
                "      where encounter_date_time is not null\n" +
                "        and is_voided is false\n" +
                "      group by date(encounter_date_time)\n" +
                "      union all\n" +
                "      select date(encounter_date_time) date, count(*) count\n" +
                "      from program_encounter\n" +
                "      where encounter_date_time is not null\n" +
                "        and is_voided is false\n" +
                "      group by date(encounter_date_time)\n" +
                "      union all\n" +
                "      select date(enrolment_date_time) date, count(*) count\n" +
                "      from program_enrolment\n" +
                "      where enrolment_date_time is not null\n" +
                "        and is_voided is false\n" +
                "      group by date(enrolment_date_time)) data\n" +
                "group by for_date\n" +
                "order by for_date asc;";
        return jdbcTemplate.query(query, new CountForDayMapper());
    }

}
