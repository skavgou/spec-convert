package com.specconvert.transformer;

import java.util.ArrayList;
import java.util.List;

// 0.8
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.api.switchconditions.EventCondition;
import io.serverlessworkflow.api.defaultdef.DefaultConditionDefinition;

// 1.0
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchItem;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;

public class Switch {
    protected static TaskItem handleSwitch(String name, SwitchState state) {
        List<SwitchItem> switchItems = new ArrayList<>();

        // Event-based conditions (eventConditions) — map eventRef as name, transition as then
        if (state.getEventConditions() != null) {
            for (EventCondition cond : state.getEventConditions()) {
                String caseName  = cond.getEventRef() != null ? util.toIdentifier(cond.getEventRef()) : "case";
                String nextState = util.transitionName(cond.getTransition());

                SwitchCase switchCase = new SwitchCase()
                        .withWhen(".received | .type == \"" + cond.getEventRef() + "\"")
                        .withThen(new FlowDirective().withString(nextState));

                switchItems.add(new SwitchItem(caseName, switchCase));
            }
        }

        // Data-based conditions (dataConditions)
        if (state.getDataConditions() != null) {
            for (DataCondition cond : state.getDataConditions()) {
                String caseName      = cond.getName() != null ? util.toIdentifier(cond.getName()) : "case";
                String rawExpression = cond.getCondition() != null ? cond.getCondition() : "TODO";
                String nextState     = util.transitionName(cond.getTransition());

                String strippedExpression = util.stripExpressionWrapper(rawExpression);
                SwitchCase switchCase = new SwitchCase()
                        .withWhen(strippedExpression)
                        .withThen(new FlowDirective().withString(nextState));

                switchItems.add(new SwitchItem(caseName, switchCase));
            }
        }

        // Default condition (no "when" predicate)
        DefaultConditionDefinition def = state.getDefaultCondition();
        if (def != null) {
            String nextState = util.transitionName(def.getTransition());
            SwitchCase defaultCase = new SwitchCase()
                    .withThen(new FlowDirective().withString(nextState));
            switchItems.add(new SwitchItem("default", defaultCase));
        }

        SwitchTask switchTask = new SwitchTask().withSwitch(switchItems);
        return new TaskItem(name, new Task().withSwitchTask(switchTask));
    }
}
