
package io.serverlessworkflow.api.states;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import javax.validation.Valid;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.error.Error;
import io.serverlessworkflow.api.filters.EventDataFilter;
import io.serverlessworkflow.api.filters.StateDataFilter;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.timeouts.TimeoutsDefinition;
import io.serverlessworkflow.api.transitions.Transition;


/**
 * This state is used to wait for events from event sources and then transitioning to a next state
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "action",
    "eventRef",
    "eventDataFilter",
    "usedForCompensation"
})
@Generated("jsonschema2pojo")
public class CallbackState
    extends DefaultState
    implements Serializable, State
{

    /**
     * Action Definition
     * 
     */
    @JsonProperty("action")
    @JsonPropertyDescription("Action Definition")
    @Valid
    private Action action;
    /**
     * References an unique callback event name in the defined workflow events
     * 
     */
    @JsonProperty("eventRef")
    @JsonPropertyDescription("References an unique callback event name in the defined workflow events")
    private java.lang.String eventRef;
    @JsonProperty("eventDataFilter")
    @Valid
    private EventDataFilter eventDataFilter;
    /**
     * If true, this state is used to compensate another state. Default is false
     * 
     */
    @JsonProperty("usedForCompensation")
    @JsonPropertyDescription("If true, this state is used to compensate another state. Default is false")
    private boolean usedForCompensation = false;
    private final static long serialVersionUID = 7392031393428885110L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public CallbackState() {
    }

    /**
     * 
     * @param name
     *     Unique name of the state.
     * @param type
     *     State type.
     */
    public CallbackState(java.lang.String name, DefaultState.Type type) {
        super(name, type);
    }

    /**
     * Action Definition
     * 
     */
    @JsonProperty("action")
    public Action getAction() {
        return action;
    }

    /**
     * Action Definition
     * 
     */
    @JsonProperty("action")
    public void setAction(Action action) {
        this.action = action;
    }

    public CallbackState withAction(Action action) {
        this.action = action;
        return this;
    }

    /**
     * References an unique callback event name in the defined workflow events
     * 
     */
    @JsonProperty("eventRef")
    public java.lang.String getEventRef() {
        return eventRef;
    }

    /**
     * References an unique callback event name in the defined workflow events
     * 
     */
    @JsonProperty("eventRef")
    public void setEventRef(java.lang.String eventRef) {
        this.eventRef = eventRef;
    }

    public CallbackState withEventRef(java.lang.String eventRef) {
        this.eventRef = eventRef;
        return this;
    }

    @JsonProperty("eventDataFilter")
    public EventDataFilter getEventDataFilter() {
        return eventDataFilter;
    }

    @JsonProperty("eventDataFilter")
    public void setEventDataFilter(EventDataFilter eventDataFilter) {
        this.eventDataFilter = eventDataFilter;
    }

    public CallbackState withEventDataFilter(EventDataFilter eventDataFilter) {
        this.eventDataFilter = eventDataFilter;
        return this;
    }

    /**
     * If true, this state is used to compensate another state. Default is false
     * 
     */
    @JsonProperty("usedForCompensation")
    public boolean isUsedForCompensation() {
        return usedForCompensation;
    }

    /**
     * If true, this state is used to compensate another state. Default is false
     * 
     */
    @JsonProperty("usedForCompensation")
    public void setUsedForCompensation(boolean usedForCompensation) {
        this.usedForCompensation = usedForCompensation;
    }

    public CallbackState withUsedForCompensation(boolean usedForCompensation) {
        this.usedForCompensation = usedForCompensation;
        return this;
    }

    @Override
    public CallbackState withId(java.lang.String id) {
        super.withId(id);
        return this;
    }

    @Override
    public CallbackState withName(java.lang.String name) {
        super.withName(name);
        return this;
    }

    @Override
    public CallbackState withType(DefaultState.Type type) {
        super.withType(type);
        return this;
    }

    @Override
    public CallbackState withEnd(End end) {
        super.withEnd(end);
        return this;
    }

    @Override
    public CallbackState withStateDataFilter(StateDataFilter stateDataFilter) {
        super.withStateDataFilter(stateDataFilter);
        return this;
    }

    @Override
    public CallbackState withMetadata(Map<String, String> metadata) {
        super.withMetadata(metadata);
        return this;
    }

    @Override
    public CallbackState withTransition(Transition transition) {
        super.withTransition(transition);
        return this;
    }

    @Override
    public CallbackState withOnErrors(List<Error> onErrors) {
        super.withOnErrors(onErrors);
        return this;
    }

    @Override
    public CallbackState withCompensatedBy(java.lang.String compensatedBy) {
        super.withCompensatedBy(compensatedBy);
        return this;
    }

    @Override
    public CallbackState withTimeouts(TimeoutsDefinition timeouts) {
        super.withTimeouts(timeouts);
        return this;
    }

}
