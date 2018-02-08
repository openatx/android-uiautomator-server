package com.github.uiautomator.stub;

import java.util.List;

public class Device {

    private String name;

    private List<Operation> operations;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }


    public class Operation {

        private String type;
        private String id;
        private Integer delay;

        private String text;

        private String value;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getDelay() {
            return delay;
        }

        public void setDelay(Integer delay) {
            this.delay = delay;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}


