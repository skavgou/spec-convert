
package io.serverlessworkflow.api.types;

import jakarta.validation.Valid;

public class ListenTask
    extends TaskBase
{

    /**
     * ListenTaskConfiguration
     * <p>
     * The configuration of the listener to use.
     * 
     */
    @Valid
    private ListenTaskConfiguration listen;
    /**
     * SubscriptionIterator
     * <p>
     * Configures the iteration over each item (event or message) consumed by a subscription.
     * 
     */
    @Valid
    private SubscriptionIterator foreach;

    /**
     * ListenTaskConfiguration
     * <p>
     * The configuration of the listener to use.
     * 
     */
    public ListenTaskConfiguration getListen() {
        return listen;
    }

    /**
     * ListenTaskConfiguration
     * <p>
     * The configuration of the listener to use.
     * 
     */
    public void setListen(ListenTaskConfiguration listen) {
        this.listen = listen;
    }

    public ListenTask withListen(ListenTaskConfiguration listen) {
        this.listen = listen;
        return this;
    }

    /**
     * SubscriptionIterator
     * <p>
     * Configures the iteration over each item (event or message) consumed by a subscription.
     * 
     */
    public SubscriptionIterator getForeach() {
        return foreach;
    }

    /**
     * SubscriptionIterator
     * <p>
     * Configures the iteration over each item (event or message) consumed by a subscription.
     * 
     */
    public void setForeach(SubscriptionIterator foreach) {
        this.foreach = foreach;
    }

    public ListenTask withForeach(SubscriptionIterator foreach) {
        this.foreach = foreach;
        return this;
    }

    @Override
    public ListenTask withIf(String _if) {
        super.withIf(_if);
        return this;
    }

    @Override
    public ListenTask withInput(Input input) {
        super.withInput(input);
        return this;
    }

    @Override
    public ListenTask withOutput(Output output) {
        super.withOutput(output);
        return this;
    }

    @Override
    public ListenTask withExport(Export export) {
        super.withExport(export);
        return this;
    }

    @Override
    public ListenTask withTimeout(TaskTimeout timeout) {
        super.withTimeout(timeout);
        return this;
    }

    @Override
    public ListenTask withThen(FlowDirective then) {
        super.withThen(then);
        return this;
    }

    @Override
    public ListenTask withMetadata(TaskMetadata metadata) {
        super.withMetadata(metadata);
        return this;
    }

}
