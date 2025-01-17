-- ORGANISATION DATA
---- Organisations and their parent
select *
from (select
        o1.id      parent_organisation_id,
        o1.name    parent_organisation_name,
        o1.db_user parent_organisation_db_user,
        o2.id      organisation_id,
        o2.name    organisation_name,
        o2.db_user organisation_db_user
      from organisation o1
        left outer join organisation o2 on o1.id = o2.parent_organisation_id) X
where parent_organisation_name = 'OpenCHS' or organisation_name is not null;

---- Organisation with address and catchment
select
  o.id organisation_id,
  c2.id catchment_id,
  c2.name catchment_name,
  address_level.id address_level_id,
  address_level.title address_level_title
from address_level
  inner join catchment_address_mapping m2 on address_level.id = m2.addresslevel_id
  inner join catchment c2 on m2.catchment_id = c2.id
  inner join organisation o on c2.organisation_id = o.id
where o.name = ?
order by c2.name;

-- ITEMS FOR TRANSLATION
select distinct name
from
  (
    select form_element_group_name as name
    from all_form_element_groups
    where organisation_id = :organisation_id
    union
    select form_element_name as name
    from all_form_elements
    where organisation_id = :organisation_id
    union
    select concept_name as name
    from all_concepts
    where organisation_id = :organisation_id
    union
    select answer_concept_name as name
    from all_concept_answers
    where organisation_id = :organisation_id
    union
    select operational_encounter_type_name as name
    from all_operational_encounter_types
    where organisation_id = :organisation_id
    union
    select encounter_type_name as name
    from all_encounter_types
    where organisation_id = :organisation_id
    union
    select operational_program_name as name
    from all_operational_programs
    where organisation_id = :organisation_id
    union
    select program_name as name
    from all_programs
    where organisation_id = :organisation_id
    union
    select name
    from checklist_detail
    where organisation_id = :organisation_id
    union
    select type as name
    from catchment
    where organisation_id = :organisation_id
    union
    select title as name
    from address_level
    where organisation_id = :organisation_id

    union

    select concept.name as name
    from concept
    where
      concept.id not in (select concept.id
                         from concept
                           inner join form_element element2 on concept.id = element2.concept_id
                         where concept.organisation_id = :organisation_id
                         union
                         select concept.id
                         from concept
                           inner join concept_answer ca on concept.id = ca.answer_concept_id
                         where concept.organisation_id = :organisation_id) and
      concept.organisation_id = :organisation_id and not concept.is_voided

    union

    select concept.name concept_name
    from concept
    where concept.id not in (select concept.id
                             from concept
                               inner join form_element element2 on concept.id = element2.concept_id
                             where concept.organisation_id = 1
                             union
                             select concept.id
                             from concept
                               inner join concept_answer ca on concept.id = ca.answer_concept_id
                             where concept.organisation_id = 1
    ) and concept.organisation_id = 1 and not concept.is_voided
  ) as X
order by name;

-- VIEW CONCEPT WITH ANSWERS
select
  concept.name,
  a.uuid  AS "Concept Answer UUID",
  c2.uuid as "Answer UUID",
  c2.name as "Answer",
  a.answer_order,
  a.organisation_id map_organisation_id,
  concept.organisation_id q_organisation_id,
  c2.organisation_id ans_organisation_id
from concept
  inner join concept_answer a on concept.id = a.concept_id
  inner join concept c2 on a.answer_concept_id = c2.id
where concept.uuid = '58b6367a-825f-43e2-b6b7-b35a5cbc3a09'
order by a.answer_order;


-- GET ALL THE FORM ELEMENTS AND CONCEPT (WITHOUT ANSWERS) IN AN ORG - (Required for translations, do not change this one)
select
  p.name,
  f.name  as FormName
  -- ,fm.entity_id
  -- ,fm.observations_type_entity_id
  -- ,fm.organisation_id
  ,
  feg.name,
  fe.name as "Form Element",
  c2.name as "Concept",
   f.organisation_id as "Organisation Id",
       fe.is_voided as "Form Element Voided"
from operational_program p
  inner join form_mapping fm on (fm.entity_id = p.program_id)
  inner join form f on fm.form_id = f.id
  inner join form_element_group feg on feg.form_id = f.id
  inner join form_element fe on fe.form_element_group_id = feg.id
  inner join concept c2 on fe.concept_id = c2.id
where p.organisation_id = ?1 and f.form_type != 'ProgramEncounterCancellation' and fe.id not in (select form_element_id
                                                                                                from non_applicable_form_element
                                                                                                where organisation_id = ?1)
order by
  f.name
  , feg.display_order asc
  , fe.display_order asc;


-- GET ALL DETAILS OF A FORM
select
--   feg.name form_element_group_name,
  fe.name as form_element_name,
  c.name as concept_name,
  c.data_type concept_data_type,
  ac.name as answer_concept_name
from form f
  inner join form_element_group feg on feg.form_id = f.id
  inner join form_element fe on fe.form_element_group_id = feg.id
  inner join concept c on fe.concept_id = c.id
  left outer join concept_answer ca on c.id = ca.concept_id
  left outer join concept ac on ca.answer_concept_id = ac.id
where f.name = ? and fe.id not in (select form_element_id from non_applicable_form_element where organisation_id = ?) and c.uuid != 'b4e5a662-97bf-4846-b9b7-9baeab4d89c4'
order by
  feg.display_order asc
  ,fe.display_order asc
  ,ca.answer_order;

-- Concept with answers
select q.name, string_agg(a.name,E'\n' order by ca.answer_order)
from concept_answer ca
inner join concept a on ca.answer_concept_id = a.id
inner join concept q on ca.concept_id = q.id
group by q.name;
---------------

------- form->form element groups->form elements->concept->answers
select
  f.name  as FormName,
  feg.name as FormElementGroup,
  CASE WHEN fe.is_mandatory=true THEN '* ' ELSE '  ' END || fe.name as "M  FormElement",
  co.name as ConceptOwn,
  feo.name as FormElementOwn,
  c.name as Concept,
  coalesce(ca.answers, '<'||c.data_type||'>') as Answers
from form f
  inner join form_element_group feg on feg.form_id = f.id
  inner join form_element fe on fe.form_element_group_id = feg.id
  inner join concept c on fe.concept_id = c.id
  inner join organisation co on co.id = c.organisation_id
  inner join organisation feo on feo.id = fe.organisation_id
  left join (
          select ca.concept_id, string_agg(a.name,E'\n' order by ca.answer_order) answers
          from concept_answer ca inner join concept a on ca.answer_concept_id = a.id
          group by ca.concept_id
      ) ca on ca.concept_id = c.id
  left join non_applicable_form_element rem on rem.form_element_id = fe.id and rem.is_voided <> TRUE and rem.organisation_id = ?
where rem.id is null and f.name = ?
order by
  f.name
  , feg.display_order asc
  , fe.display_order asc;
--------------

-- Get all the REGISTRATION form elements and concept (without answers) for translation
select
  f.name  as FormName
  -- ,fm.entity_id
  -- ,fm.observations_type_entity_id
  -- ,fm.organisation_id
  ,
  feg.name,
  fe.name as "Form Element",
  c2.name as "Concept"
from form f
  inner join form_element_group feg on feg.form_id = f.id
  inner join form_element fe on fe.form_element_group_id = feg.id
  inner join concept c2 on fe.concept_id = c2.id
where f.organisation_id = 9 and f.form_type = 'IndividualProfile'
order by
  f.name
  , feg.display_order asc
  , fe.display_order asc;

-- Get Programs
select
  operational_program.name,
  p.name
from operational_program
  inner join program p on operational_program.program_id = p.id
where operational_program.organisation_id = :org_name;

-- Encounter types
select
  et.name  "EncounterType",
  oet.name "OrgEncounterType"
from operational_encounter_type oet
  inner join encounter_type et on oet.encounter_type_id = et.id;

-- Program with its encounter types
select
  distinct
  operational_program.name operational_program_name,
  p.name program_name,
  form.id form_id,
  form.name form_name,
  encounter_type.name encouter_type_name
from operational_program
  inner join program p on operational_program.program_id = p.id
  inner join form_mapping on form_mapping.entity_id = p.id
  inner join form on form_mapping.form_id = form.id
  left outer join encounter_type on encounter_type.id = form_mapping.observations_type_entity_id
where operational_program.organisation_id = ?
order by operational_program_name, program_name, form_name;


-- Cancel Forms
select
  f2.id               as FormMappingId,
  program.name        as Program,
  encounter_type.name as EncounterType
from form f
  inner join form_mapping f2 on f.id = f2.form_id
  inner join encounter_type on encounter_type.id = f2.observations_type_entity_id
  inner join program on program.id = f2.entity_id
where f2.organisation_id = 2 and f.form_type = 'ProgramEncounterCancellation'
order by
  Program;


select
  i.id,
  audit.last_modified_date_time
from address_level
  inner join catchment_address_mapping m2 on address_level.id = m2.addresslevel_id
  inner join individual i on address_level.id = i.address_id
  inner join catchment on catchment.id = m2.catchment_id
  inner join program_enrolment on i.id = program_enrolment.individual_id
  inner join audit on audit.id = i.audit_id
where m2.catchment_id = 2
order by audit.last_modified_date_time desc;


select distinct i.id
from address_level
  inner join catchment_address_mapping m2 on address_level.id = m2.addresslevel_id
  inner join individual i on address_level.id = i.address_id
  inner join catchment on catchment.id = m2.catchment_id
  left outer join program_enrolment on i.id = program_enrolment.individual_id
where m2.catchment_id = 2 and program_enrolment.id is null;


-- Queries to test and fix the audit last_modified_by updating with wrong user issue. Card #982 will provide a solution.
-- Query to run after implementation deployment. The count is desired to be 0
select count(a.id) from form_element x
  inner join audit a on x.audit_id = a.id
where x.organisation_id = 1 and a.last_modified_by_id != 1;

-- Query to fix
update audit set last_modified_by_id = 1 where id in (select a.id from form_element x
  inner join audit a on x.audit_id = a.id
where x.organisation_id = 1 and a.last_modified_by_id != 1
);

select count(a.id) from audit a
where a.last_modified_by_id = 1 and a.created_by_id != 1;

update audit set created_by_id = 1 where last_modified_by_id = 1;

--This query shows an example of how to update audit table after updating the entity
with updates(audit_id) as (
  update individual
  set is_voided = true
  where false
  returning audit_id
)
update audit
set last_modified_date_time = current_timestamp
where id in (select audit_id from updates);

-- Find long running queries
SELECT
    pid,
    now() - pg_stat_activity.query_start AS duration,
    query,
    state
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes';

-- See what queries are in waiting to acquire locks
SELECT relation::regclass, * FROM pg_locks WHERE NOT GRANTED;

-- See what processes are blocking what queries (these only find row-level locks, not object-level locks)
SELECT blocked_locks.pid     AS blocked_pid,
       blocked_activity.usename  AS blocked_user,
       blocking_locks.pid     AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query    AS blocked_statement,
       blocking_activity.query   AS current_statement_in_blocking_process
FROM  pg_catalog.pg_locks         blocked_locks
        JOIN pg_catalog.pg_stat_activity blocked_activity  ON blocked_activity.pid = blocked_locks.pid
        JOIN pg_catalog.pg_locks         blocking_locks
             ON blocking_locks.locktype = blocked_locks.locktype
               AND blocking_locks.DATABASE IS NOT DISTINCT FROM blocked_locks.DATABASE
               AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
               AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
               AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
               AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
               AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
               AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
               AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
               AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
               AND blocking_locks.pid != blocked_locks.pid

        JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.GRANTED;


-- To get Form element groups, form element,isMandatory, datatype and concept answers for form with specified id
select form_group.name "Form element group", 
       group_element.name "Form element",
       concept.data_type::text "Datatype",
       (is_mandatory)::text  "Is mandatory",
        string_agg(distinct answer_concept_name,',') "Concept answers"
       

from form_element_group form_group

left join form_element group_element on group_element.form_element_group_id = form_group.id
left join concept concept on concept.id = group_element.concept_id
left join concept_concept_answer concept_answer on concept_answer.concept_uuid = concept.uuid

where form_id = '480' and form_group.is_voided = false and group_element.is_voided= false
group by  form_group.name,group_element.name, concept.data_type,is_mandatory, form_group.display_order,group_element.display_order
order by  form_group.display_order, group_element.display_order;


--- To find out if there are audits of any meta data not belonging to the given org
--- Useful when a metadata http sync request is returning truncated data
select fea.last_modified_by_id,
       fe.id,
       fe.name,
       fe.is_voided,
       fega.last_modified_by_id,
       fa.last_modified_by_id,
       ca.last_modified_by_id
from form_element fe
       join form_element_group feg on fe.form_element_group_id = feg.id
       join form f on feg.form_id = f.id
       join concept c on fe.concept_id = c.id
       join audit fea on fea.id = fe.audit_id
       join audit fega on fega.id = feg.audit_id
       join audit fa on fa.id = f.audit_id
       join audit ca on ca.id = c.audit_id
where fe.organisation_id = :orgId
  and (fega.last_modified_by_id in (select id from users where organisation_id != :orgId and organisation_id is not null)
  or fea.last_modified_by_id in (select id from users where organisation_id != :orgId and organisation_id is not null)
  or fa.last_modified_by_id in (select id from users where organisation_id != :orgId and organisation_id is not null)
  or ca.last_modified_by_id in (select id from users where organisation_id != :orgId and organisation_id is not null));
                                
 --- To get a full list of locations with respect herierachies
 --- Update the type_id as per the address_level_type setup done for the org
 with state as (select address_level.title as name, id from address_level where type_id = 85 and address_level.is_voided = false),
     city as (select address_level.title as name, id, parent_id from address_level where type_id = 86 and address_level.is_voided = false),
     zone as (select address_level.title as name, id, parent_id from address_level where type_id = 87 and address_level.is_voided = false),
     taluka as (select address_level.title as name, id, parent_id from address_level where type_id = 268 and address_level.is_voided = false),
     ward as (select address_level.title as name, id, parent_id from address_level where type_id = 88 and address_level.is_voided = false)

select state.name as "State",
       city.name as "City",
       zone.name as "Zone",
       taluka.name as "Taluka",
       ward.name as "Ward"
from state
           left join city on city.parent_id = state.id
           left join zone on zone.parent_id = city.id
           left join taluka on taluka.parent_id = city.id
           left join ward on ward.parent_id = zone.id;


-- DELETE VOIDED FORMS
set role jsscp;
delete from form_element where id in (
     select fe.id from form_element fe
   join form_element_group feg on feg.id = fe.form_element_group_id
   join form f on feg.form_id = f.id
where f.organisation_id = 22 and f.is_voided = true
     );

delete from form_element_group where id in (
     select feg.id from form_element_group feg
          join form f on feg.form_id = f.id
     where f.organisation_id = 22 and f.is_voided = true
     );

delete from form_mapping where id in (
 select fm.id from form_mapping fm
       join form f on f.id = fm.form_id
 where f.is_voided = true and f.organisation_id = 22
 );

delete from form where id in (
     select id from form f
     where f.is_voided = true and f.organisation_id = 22
     );

-- Finding Duplicate Audits -- Finding
select audit.last_modified_date_time, entity.organisation_id, count(1)
from program_enrolment entity
         join audit on entity.audit_id = audit.id
where entity.organisation_id = ?
group by audit.last_modified_date_time, entity.organisation_id
having count(1) > 1;

-- Finding Duplicate Audits -- Dry Run Fixing Duplicate Audits
select last_modified_date_time + a.id * ('1 millisecond' :: interval)
from audit a join program_enrolment entity on a.id = entity.audit_id
where 1 = 1
  and a.last_modified_date_time = ?
  and entity.organisation_id = ?
order by 1 desc;

-- Finding Duplicate Audits -- Fixing Duplicate Audits
update audit a
set last_modified_date_time = last_modified_date_time + a.id * ('1 millisecond' :: interval)
from program_enrolment entity
where entity.audit_id = a.id
  and entity.organisation_id = ?
  and a.last_modified_date_time = ?;

-- Find out scheduled jobs in the system

select * from
    (select bje.job_execution_id execution_id,
            bje.status status,
            bje.exit_code exit_code,
            bje.create_time create_time,
            bje.start_time start_time,
            bje.end_time end_time,
            string_agg(case when bjep.key_name = 'uuid' then bjep.string_val else '' end::text, '') uuid,
            string_agg(case when bjep.key_name = 'fileName' then bjep.string_val else '' end::text, '') fileName,
            sum(case when bjep.key_name = 'noOfLines' then bjep.long_val else 0 end) noOfLines,
            string_agg(case when bjep.key_name = 's3Key' then bjep.string_val else '' end::text, '') s3Key,
            sum(case when bjep.key_name = 'userId' then bjep.long_val else 0 end) userId,
            string_agg(case when bjep.key_name = 'type' then bjep.string_val::text else '' end::text, '') job_type,
            max(case when bjep.key_name = 'startDate' then bjep.date_val::timestamp else null::timestamp end::timestamp) startDate,
            max(case when bjep.key_name = 'endDate' then bjep.date_val::timestamp else null::timestamp end::timestamp) endDate,
            string_agg(case when bjep.key_name = 'subjectTypeUUID' then bjep.string_val::text else '' end::text, '') subjectTypeUUID,
            string_agg(case when bjep.key_name = 'programUUID' then bjep.string_val::text else '' end::text, '') programUUID,
            string_agg(case when bjep.key_name = 'encounterTypeUUID' then bjep.string_val::text else '' end::text, '') encounterTypeUUID,
            string_agg(case when bjep.key_name = 'reportType' then bjep.string_val::text else '' end::text, '') reportType,
            max(bse.read_count) read_count,
            max(bse.write_count) write_count,
            max(bse.write_skip_count) write_skip_count
     from batch_job_execution bje
              left outer join  batch_job_execution_params bjep on bje.job_execution_id = bjep.job_execution_id
              left outer join batch_step_execution bse on bje.job_execution_id = bse.job_execution_id
     group by bje.job_execution_id, bje.status, bje.exit_code, bje.create_time, bje.start_time, bje.end_time
     order by bje.create_time desc) jobs;