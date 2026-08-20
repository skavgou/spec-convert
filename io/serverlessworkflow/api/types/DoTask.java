
package io.serverlessworkflow.api.types;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;

public class DoTask
    extends TaskBase
{

    /**
     * TaskList
     * <p>
     * List of named tasks to perform.
     * 
     */
    private List<@Valid TaskItem> _do = new ArrayList<TaskItem>();

    /**
     * TaskList
     * <p>
     * List of named tasks to perform.
     * 
     */
    public List<TaskItem> getDo() {
        return _do;
    }

    /**
     * TaskList
     * <p>
     * List of named tasks to perform.
     * 
     */
    public void setDo(List<TaskItem> _do) {
        this._do = _do;
    }

    public DoTask withDo(List<TaskItem> _do) {
        this._do = _do;
        return this;
    }

    @Override
    public DoTask withIf(String _if) {
        super.withIf(_if);
        return this;
    }

    @Override
    public DoTask withInput(Input input) {
        super.withInput(input);
        return this;
    }

    @Override
    public DoTask withOutput(Output output) {
        super.withOutput(output);
        return this;
    }

    @Override
    public DoTask withExport(Export export) {
        super.withExport(export);
        return this;
    }

    @Override
    public DoTask withTimeout(TaskTimeout timeout) {
        super.withTimeout(timeout);
        return this;
    }

    @Override
    public DoTask withThen(FlowDirective then) {
        super.withThen(then);
        return this;
    }

    @Override
    public DoTask withMetadata(TaskMetadata metadata) {
        super.withMetadata(metadata);
        return this;
    }

}
