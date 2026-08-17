package com.specconvert.transformer;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 0.8
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.branches.Branch;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.events.OnEvents;
import io.serverlessworkflow.api.mapper.JsonObjectMapper;
import io.serverlessworkflow.api.mapper.YamlObjectMapper;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.api.states.InjectState;
import io.serverlessworkflow.api.states.ParallelState;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.states.SleepState;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.api.switchconditions.EventCondition;
import io.serverlessworkflow.api.defaultdef.DefaultConditionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.transitions.Transition;

// 1.0
import io.serverlessworkflow.api.types.AllEventConsumptionStrategy;
import io.serverlessworkflow.api.types.AnyEventConsumptionStrategy;
import io.serverlessworkflow.api.types.CallFunction;
import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.DurationInline;
import io.serverlessworkflow.api.types.EventData;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.ForkTask;
import io.serverlessworkflow.api.types.ForkTaskConfiguration;
import io.serverlessworkflow.api.types.FunctionArguments;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.ListenTaskConfiguration;
import io.serverlessworkflow.api.types.ListenTo;
import io.serverlessworkflow.api.types.SubscriptionIterator;
import io.serverlessworkflow.api.types.Set;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.SetTaskConfiguration;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchItem;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TimeoutAfter;
import io.serverlessworkflow.api.types.WaitTask;
import io.serverlessworkflow.api.WorkflowFormat;
import io.serverlessworkflow.api.WorkflowWriter;
import java.util.HashMap;
import java.util.Map;

import com.specconvert.transformer.*;

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
