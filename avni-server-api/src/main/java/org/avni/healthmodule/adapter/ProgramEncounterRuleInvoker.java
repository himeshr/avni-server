package org.avni.healthmodule.adapter;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.avni.dao.ConceptRepository;
import org.avni.healthmodule.adapter.contract.DecisionRuleResponse;
import org.avni.healthmodule.adapter.contract.ProgramScheduledVisitsResponse;
import org.avni.healthmodule.adapter.contract.encounter.ProgramEncounterDecisionRuleResponse;
import org.avni.healthmodule.adapter.contract.encounter.ProgramEncounterRuleInput;
import org.avni.web.request.ObservationRequest;
import org.avni.web.request.ProgramEncounterRequest;

import javax.script.ScriptEngine;
import java.io.InputStream;
import java.util.List;

public class ProgramEncounterRuleInvoker extends HealthModuleInvoker {
    public ProgramEncounterRuleInvoker(ScriptEngine scriptEngine, InputStream inputStream) {
        super(scriptEngine, inputStream);
    }

    public List<ObservationRequest> getDecisions(ProgramEncounterRuleInput ruleInput, ConceptRepository conceptRepository) {
        ScriptObjectMirror decision = (ScriptObjectMirror) this.invoke("getDecisions", ruleInput);
        ProgramEncounterDecisionRuleResponse decisionRuleResponse = new ProgramEncounterDecisionRuleResponse(decision);
        List<DecisionRuleResponse> decisionRuleResponses = decisionRuleResponse.getDecisionRuleResponses();
        return getObservationRequests(conceptRepository, decisionRuleResponses);
    }

    public List<ProgramEncounterRequest> getNextScheduledVisits(ProgramEncounterRuleInput ruleInput, String enrolmentUUID) {
        ScriptObjectMirror nextScheduledVisits = (ScriptObjectMirror) this.invoke("getNextScheduledVisits", ruleInput);
        ProgramScheduledVisitsResponse programEnrolmentNextScheduledVisitsResponse = new ProgramScheduledVisitsResponse(nextScheduledVisits);
        return programEnrolmentNextScheduledVisitsResponse.getProgramEncounterRequests(enrolmentUUID);
    }
}
