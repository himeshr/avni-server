package org.avni.healthmodule.adapter.contract.enrolment;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.avni.healthmodule.adapter.contract.DecisionRuleResponse;
import org.avni.healthmodule.adapter.contract.RuleResponse;

import java.util.ArrayList;
import java.util.List;

public class ProgramEnrolmentDecisionRuleResponse extends RuleResponse {
    private List<DecisionRuleResponse> decisionRuleResponses = new ArrayList<>();

    public ProgramEnrolmentDecisionRuleResponse(ScriptObjectMirror scriptObjectMirror) {
        super(scriptObjectMirror);
        ScriptObjectMirror enrolmentDecisions = (ScriptObjectMirror) scriptObjectMirror.get("enrolmentDecisions");
        addToList(enrolmentDecisions, this.decisionRuleResponses, object -> new DecisionRuleResponse((ScriptObjectMirror) object));
    }

    public List<DecisionRuleResponse> getDecisionRuleResponses() {
        return decisionRuleResponses;
    }
}
