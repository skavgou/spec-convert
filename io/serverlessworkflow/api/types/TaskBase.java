
package io.serverlessworkflow.api.types;

import java.io.Serializable;
import javax.annotation.processing.Generated;
import jakarta.validation.Valid;


/**
 * TaskBase
 * <p>
 * An object inherited by all tasks.
 * 
 */
@Generated("jsonschema2pojo")
public class TaskBase implements Serializable
{

    /**
     * TaskBaseIf
     * <p>
     * A runtime expression, if any, used to determine whether or not the task should be run.
     * 
     */
    private String _if;
    /**
     * Input
     * <p>
     * Configures the input of a workflow or task.
     * 
     */
    @Valid
    private Input input;
    /**
     * Output
     * <p>
     * Configures the output of a workflow or task.
     * 
     */
    @Valid
    private Output output;
    /**
     * Export
     * <p>
     * Set the content of the context. .
     * 
     */
    @Valid
    private Export export;
    /**
     * TaskTimeout
     * <p>
     * 
     * 
     */
    private TaskTimeout timeout;
    /**
     * FlowDirective
     * <p>
     * Represents different transition options for a workflow.
     * 
     */
    private FlowDirective then;
    /**
     * TaskMetadata
     * <p>
     * Holds additional information about the task.
     * 
     */
    @Valid
    private TaskMetadata metadata;
    private final static long serialVersionUID = -2383885712873823411L;

    /**
     * TaskBaseIf
     * <p>
     * A runtime expression, if any, used to determine whether or not the task should be run.
     * 
     */
    public String getIf() {
        return _if;
    }

    /**
     * TaskBaseIf
     * <p>
     * A runtime expression, if any, used to determine whether or not the task should be run.
     * 
     */
    public void setIf(String _if) {
        this._if = _if;
    }

    public TaskBase withIf(String _if) {
        this._if = _if;
        return this;
    }

    /**
     * Input
     * <p>
     * Configures the input of a workflow or task.
     * 
     */
    public Input getInput() {
        return input;
    }

    /**
     * Input
     * <p>
     * Configures the input of a workflow or task.
     * 
     */
    public void setInput(Input input) {
        this.input = input;
    }

    public TaskBase withInput(Input input) {
        this.input = input;
        return this;
    }

    /**
     * Output
     * <p>
     * Configures the output of a workflow or task.
     * 
     */
    public Output getOutput() {
        return output;
    }

    /**
     * Output
     * <p>
     * Configures the output of a workflow or task.
     * 
     */
    public void setOutput(Output output) {
        this.output = output;
    }

    public TaskBase withOutput(Output output) {
        this.output = output;
        return this;
    }

    /**
     * Export
     * <p>
     * Set the content of the context. .
     * 
     */
    public Export getExport() {
        return export;
    }

    /**
     * Export
     * <p>
     * Set the content of the context. .
     * 
     */
    public void setExport(Export export) {
        this.export = export;
    }

    public TaskBase withExport(Export export) {
        this.export = export;
        return this;
    }

    /**
     * TaskTimeout
     * <p>
     * 
     * 
     */
    public TaskTimeout getTimeout() {
        return timeout;
    }

    /**
     * TaskTimeout
     * <p>
     * 
     * 
     */
    public void setTimeout(TaskTimeout timeout) {
        this.timeout = timeout;
    }

    public TaskBase withTimeout(TaskTimeout timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * FlowDirective
     * <p>
     * Represents different transition options for a workflow.
     * 
     */
    public FlowDirective getThen() {
        return then;
    }

    /**
     * FlowDirective
     * <p>
     * Represents different transition options for a workflow.
     * 
     */
    public void setThen(FlowDirective then) {
        this.then = then;
    }

    public TaskBase withThen(FlowDirective then) {
        this.then = then;
        return this;
    }

    /**
     * TaskMetadata
     * <p>
     * Holds additional information about the task.
     * 
     */
    public TaskMetadata getMetadata() {
        return metadata;
    }

    /**
     * TaskMetadata
     * <p>
     * Holds additional information about the task.
     * 
     */
    public void setMetadata(TaskMetadata metadata) {
        this.metadata = metadata;
    }

    public TaskBase withMetadata(TaskMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

}
