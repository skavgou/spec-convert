package com.specconvert.transformer;

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


public class Sleep {
    /**
     * Parse an ISO 8601 duration string (e.g. "P2DT3H4M") into total seconds,
     * then build a 1.0 wait block:
     * { "wait": { "seconds": 183840 } }
     */
    protected static WaitTask handleWait(String iso8601Duration) {
        Pattern pattern = Pattern.compile(
            "P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?"
        );
        Matcher m = pattern.matcher(iso8601Duration);

        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid ISO 8601 duration: " + iso8601Duration);
        }

        long years   = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
        long months  = m.group(2) != null ? Long.parseLong(m.group(2)) : 0;
        long days    = m.group(3) != null ? Long.parseLong(m.group(3)) : 0;
        long  hours   = m.group(4) != null ? Integer.parseInt(m.group(4)) : 0;
        long  minutes = m.group(5) != null ? Integer.parseInt(m.group(5)) : 0;
        long  seconds = m.group(6) != null ? Integer.parseInt(m.group(6)) : 0;

        // Fold years/months into days (approximate)
        long totalDays = days + years * 365L + months * 30L;

        // Convert each unit type to seconds
        years *= 365 * 86400;
        months *= 30 * 86400;
        days *= 86400;
        hours *= 3600;
        minutes *= 60;
        int totalSeconds = (int) (totalDays + hours + minutes + seconds);

        DurationInline dur = new DurationInline()
                .withSeconds(totalSeconds);

        return new WaitTask().withWait(new TimeoutAfter().withDurationInline(dur));
    }
}
