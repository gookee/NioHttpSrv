package com.core.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParameterList {
    private List<Parameter> list = new ArrayList<>();

    public List<Parameter> getList() {
        return list;
    }

    public void put(String key, String value) {
        Optional<Parameter> optional = list.parallelStream().filter(c -> c.getCaseInsensitiveKey().equals(key.toUpperCase())).findFirst();
        if (optional.isPresent()) {
            Parameter parameter = optional.get();
            parameter.setValue(parameter.getValue() + "," + value);
        } else
            list.add(new Parameter(key, value));
    }

    public String get(String key) {
        Optional<Parameter> optional = list.parallelStream().filter(c -> c.getCaseInsensitiveKey().equals(key.toUpperCase())).findFirst();
        if (optional.isPresent()) {
            Parameter parameter = optional.get();
            return parameter.getValue();
        } else
            return "";
    }

    public class Parameter {
        private String caseInsensitiveKey;
        private String key;
        private String value;

        public Parameter(String key, String value) {
            this.caseInsensitiveKey = key.toUpperCase();
            this.key = key;
            this.value = value;
        }

        public String getCaseInsensitiveKey() {
            return caseInsensitiveKey;
        }

        public void setCaseInsensitiveKey(String caseInsensitiveKey) {
            this.caseInsensitiveKey = caseInsensitiveKey;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
