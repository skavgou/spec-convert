
package io.serverlessworkflow.api.states;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.error.Error;
import io.serverlessworkflow.api.filters.StateDataFilter;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.timeouts.TimeoutsDefinition;
import io.serverlessworkflow.api.transitions.Transition;


/**
 * Default State
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "type",
    "end",
    "stateDataFilter",
    "metadata",
    "transition",
    "onErrors",
    "compensatedBy",
    "timeouts"
})
@Generated("jsonschema2pojo")
public class DefaultState implements Serializable, State
{

    /**
     * State unique identifier
     * 
     */
    @JsonProperty("id")
    @JsonPropertyDescription("State unique identifier")
    @Size(min = 1)
    private java.lang.String id;
    /**
     * Unique name of the state
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Unique name of the state")
    @Size(min = 1)
    @NotNull
    private java.lang.String name;
    /**
     * State type
     * (Required)
     * 
     */
    @JsonProperty("type")
    @JsonPropertyDescription("State type")
    @NotNull
    private DefaultState.Type type;
    /**
     * State end definition
     * 
     */
    @JsonProperty("end")
    @JsonPropertyDescription("State end definition")
    @Valid
    private End end;
    @JsonProperty("stateDataFilter")
    @Valid
    private StateDataFilter stateDataFilter;
    /**
     * Metadata
     * 
     */
    @JsonProperty("metadata")
    @JsonPropertyDescription("Metadata")
    @Valid
    private Map<String, String> metadata;
    @JsonProperty("transition")
    @Valid
    private Transition transition;
    /**
     * State error handling definitions
     * 
     */
    @JsonProperty("onErrors")
    @JsonPropertyDescription("State error handling definitions")
    @Valid
    private List<Error> onErrors = new ArrayList<Error>();
    /**
     * Unique Name of a workflow state which is responsible for compensation of this state
     * 
     */
    @JsonProperty("compensatedBy")
    @JsonPropertyDescription("Unique Name of a workflow state which is responsible for compensation of this state")
    @Size(min = 1)
    private java.lang.String compensatedBy;
    /**
     * Timeouts Definition
     * 
     */
    @JsonProperty("timeouts")
    @JsonPropertyDescription("Timeouts Definition")
    @Valid
    private TimeoutsDefinition timeouts;
    private final static long serialVersionUID = 4664857973128584689L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public DefaultState() {
    }

    /**
     * 
     * @param name
     *     Unique name of the state.
     * @param type
     *     State type.
     */
    public DefaultState(java.lang.String name, DefaultState.Type type) {
        super();
        this.name = name;
        this.type = type;
    }

    /**
     * State unique identifier
     * 
     */
    @JsonProperty("id")
    public java.lang.String getId() {
        return id;
    }

    /**
     * State unique identifier
     * 
     */
    @JsonProperty("id")
    public void setId(java.lang.String id) {
        this.id = id;
    }

    public DefaultState withId(java.lang.String id) {
        this.id = id;
        return this;
    }

    /**
     * Unique name of the state
     * (Required)
     * 
     */
    @JsonProperty("name")
    public java.lang.String getName() {
        return name;
    }

    /**
     * Unique name of the state
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(java.lang.String name) {
        this.name = name;
    }

    public DefaultState withName(java.lang.String name) {
        this.name = name;
        return this;
    }

    /**
     * State type
     * (Required)
     * 
     */
    @JsonProperty("type")
    public DefaultState.Type getType() {
        return type;
    }

    /**
     * State type
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(DefaultState.Type type) {
        this.type = type;
    }

    public DefaultState withType(DefaultState.Type type) {
        this.type = type;
        return this;
    }

    /**
     * State end definition
     * 
     */
    @JsonProperty("end")
    public End getEnd() {
        return end;
    }

    /**
     * State end definition
     * 
     */
    @JsonProperty("end")
    public void setEnd(End end) {
        this.end = end;
    }

    public DefaultState withEnd(End end) {
        this.end = end;
        return this;
    }

    @JsonProperty("stateDataFilter")
    public StateDataFilter getStateDataFilter() {
        return stateDataFilter;
    }

    @JsonProperty("stateDataFilter")
    public void setStateDataFilter(StateDataFilter stateDataFilter) {
        this.stateDataFilter = stateDataFilter;
    }

    public DefaultState withStateDataFilter(StateDataFilter stateDataFilter) {
        this.stateDataFilter = stateDataFilter;
        return this;
    }

    /**
     * Metadata
     * 
     */
    @JsonProperty("metadata")
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Metadata
     * 
     */
    @JsonProperty("metadata")
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public DefaultState withMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    @JsonProperty("transition")
    public Transition getTransition() {
        return transition;
    }

    @JsonProperty("transition")
    public void setTransition(Transition transition) {
        this.transition = transition;
    }

    public DefaultState withTransition(Transition transition) {
        this.transition = transition;
        return this;
    }

    /**
     * State error handling definitions
     * 
     */
    @JsonProperty("onErrors")
    public List<Error> getOnErrors() {
        return onErrors;
    }

    /**
     * State error handling definitions
     * 
     */
    @JsonProperty("onErrors")
    public void setOnErrors(List<Error> onErrors) {
        this.onErrors = onErrors;
    }

    public DefaultState withOnErrors(List<Error> onErrors) {
        this.onErrors = onErrors;
        return this;
    }

    /**
     * Unique Name of a workflow state which is responsible for compensation of this state
     * 
     */
    @JsonProperty("compensatedBy")
    public java.lang.String getCompensatedBy() {
        return compensatedBy;
    }

    /**
     * Unique Name of a workflow state which is responsible for compensation of this state
     * 
     */
    @JsonProperty("compensatedBy")
    public void setCompensatedBy(java.lang.String compensatedBy) {
        this.compensatedBy = compensatedBy;
    }

    public DefaultState withCompensatedBy(java.lang.String compensatedBy) {
        this.compensatedBy = compensatedBy;
        return this;
    }

    /**
     * Timeouts Definition
     * 
     */
    @JsonProperty("timeouts")
    public TimeoutsDefinition getTimeouts() {
        return timeouts;
    }

    /**
     * Timeouts Definition
     * 
     */
    @JsonProperty("timeouts")
    public void setTimeouts(TimeoutsDefinition timeouts) {
        this.timeouts = timeouts;
    }

    public DefaultState withTimeouts(TimeoutsDefinition timeouts) {
        this.timeouts = timeouts;
        return this;
    }


    /**
     * State type
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Type {

        EVENT("event"),
        OPERATION("operation"),
        SWITCH("switch"),
        SLEEP("sleep"),
        PARALLEL("parallel"),
        SUBFLOW("subflow"),
        INJECT("inject"),
        FOREACH("foreach"),
        CALLBACK("callback");
        private final java.lang.String value;
        private final static Map<java.lang.String, DefaultState.Type> CONSTANTS = new HashMap<java.lang.String, DefaultState.Type>();

        static {
            for (DefaultState.Type c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Type(java.lang.String value) {
            this.value = value;
        }

        @Override
        public java.lang.String toString() {
            return this.value;
        }

        @JsonValue
        public java.lang.String value() {
            return this.value;
        }

        @JsonCreator
        public static DefaultState.Type fromValue(java.lang.String value) {
            DefaultState.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
