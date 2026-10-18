package com.am.mcp.contract;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Spring AI {@link ToolCallback} backed by an am-specs contract tool.
 */
public final class ContractToolCallback implements ToolCallback {

    private final ContractToolSpec spec;
    private final ContractHttpExecutor executor;
    private final ToolDefinition definition;

    public ContractToolCallback(ContractToolSpec spec, ContractHttpExecutor executor) {
        this.spec = spec;
        this.executor = executor;
        this.definition = ToolDefinition.builder()
                .name(spec.name())
                .description(spec.description())
                .inputSchema(spec.inputSchemaJson())
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }

    @Override
    public String call(String toolInput) {
        return executor.execute(spec, toolInput);
    }

    public ContractToolSpec spec() {
        return spec;
    }
}
