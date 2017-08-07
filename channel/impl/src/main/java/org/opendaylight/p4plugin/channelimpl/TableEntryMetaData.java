/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.channelimpl;

import java.util.LinkedHashMap;

public class TableEntryMetaData {
    private Integer deviceId;
    private UpdateType updateType;
    private MatchType matchType;
    private String tableName;
    private String fieldName;
    private String matchValue;
    private Integer prefixLen;
    private String actionName;
    private LinkedHashMap<String, String> params;

    private TableEntryMetaData() {}

    public Integer getDeviceId() {
        return deviceId;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public String getTableName() {
        return tableName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMatchValue() {
        return matchValue;
    }

    public String getActionName() {
        return actionName;
    }

    public LinkedHashMap<String, String>  getParams() {
        return params;
    }

    public Integer getPrefixLen() {
        return prefixLen;
    }

    enum MatchType {
        EXACT,
        TERNARY,
        LPM,
        RANGE,
        VALID
    }

    enum UpdateType {
        UNSPECIFIED,
        INSERT,
        MODIFY,
        DELETE
    }

    /**
     * address must be in decimal and separated by dot, like 10.0.0.10;
     * mac address must be in hexadecimal and separated by colon, like 01:02:03:0a:0b:0c;
     * prefiexlen must be in decimal;
     */
    public static final class Builder {
        private Integer deviceId_;
        private UpdateType updateType_;
        private MatchType matchType_;
        private String tableName_;
        private String fieldName_;
        private String matchValue_;
        private Integer prefixLen_;
        private String actionName_;
        private LinkedHashMap<String, String> params_ = new LinkedHashMap<>();

        public Builder() {}
        public Builder setDeviceId(Integer deviceId) {
            deviceId_ = deviceId;
            return this;
        }

        public Builder setUpdateType(UpdateType type) {
            updateType_ = type;
            return this;
        }

        public Builder setMatchType(MatchType type) {
            matchType_ = type;
            return this;
        }

        public Builder setTableName(String tableName) {
            tableName_ = tableName;
            return this;
        }

        public Builder setFieldName(String fieldName) {
            fieldName_ = fieldName;
            return this;
        }

        public Builder setMatchValue(String value) {
            matchValue_ = value;
            return this;
        }

        public Builder setPrefixLen(Integer prefixLen) {
            prefixLen_ = prefixLen;
            return this;
        }

        public Builder setActionName(String actionName) {
            actionName_ = actionName;
            return this;
        }

        public Builder addParams(String key, String value) {
            params_.put(key, value);
            return this;
        }

        public TableEntryMetaData build() {
            TableEntryMetaData entry = new TableEntryMetaData();
            entry.deviceId = deviceId_;
            entry.matchType = matchType_;
            entry.matchValue = matchValue_;
            entry.updateType = updateType_;
            entry.actionName = actionName_;
            entry.fieldName = fieldName_;
            entry.params = params_;
            entry.prefixLen = prefixLen_;
            entry.tableName = tableName_;
            return entry;
        }
    }
}
