package JGroupsProject.Operations;

import java.io.Serializable;

public class ComplexMessage implements Serializable {
    private String key;
    private Integer value;
    private Operation operation;

    public ComplexMessage(String key, Integer value, Operation operation) {
        this.key = key;
        this.value = value;
        this.operation = operation;
    }

    public ComplexMessage(String key, Operation operation) {
        this.key = key;
        this.operation = operation;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public String getKey() {
        return key;
    }

    public Integer getValue() {
        return value;
    }
}
