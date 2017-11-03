/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.table;

import org.opendaylight.p4plugin.core.impl.device.DeviceManager;
import org.opendaylight.p4plugin.core.impl.device.P4Device;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.ActionProfileGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.GroupKey;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ActionProfileGroupOperator {
    protected P4Device device;
    protected Update.Type type;
    protected org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup group;

    private ActionProfileGroupOperator(String nodeId) {
        this.device = DeviceManager.getInstance().findConfiguredDevice(nodeId);
    }

    private ActionProfileGroupOperator(String nodeId, Update.Type type) {
        this(nodeId);
        this.type = type;
    }

    /**
     * This constructor is used to add and modify a group.
     */
    private ActionProfileGroupOperator(String nodeId, Update.Type type, ActionProfileGroup group) {
        this(nodeId, type);
        this.group = device.toMessage(group);
    }

    /**
     * This constructor is used to delete a group.
     */
    private ActionProfileGroupOperator(String nodeId, Update.Type type, GroupKey group) {
        this(nodeId, type);
        this.group = device.toMessage(group);
    }

    /**
     * Write a action profile group actually.
     */
    public boolean operate() {
        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        Update.Builder updateBuilder = Update.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileGroup(group);
        updateBuilder.setEntity(entityBuilder);
        updateBuilder.setType(type);
        requestBuilder.addUpdates(updateBuilder);
        requestBuilder.setDeviceId(device.getDeviceId());
        return device.write(requestBuilder.build()) != null;
    }

    /**
     * Add a action profile group, update type is INSERT, including create a group and
     * add group member.
     */
    public static class AddActionProfileGroupOperator extends ActionProfileGroupOperator {
        public AddActionProfileGroupOperator(String nodeId, ActionProfileGroup group) {
            super(nodeId, Update.Type.INSERT, group);
        }
    }

    /**
     * Delete a action profile group, update type is DELETE. Only action profile name and
     * group id are required to delete a group.
     */
    public static class DeleteActionProfileGroupOperator extends ActionProfileGroupOperator {
        public DeleteActionProfileGroupOperator(String nodeId, GroupKey group) {
            super(nodeId, Update.Type.DELETE, group);
        }
    }

    /**
     * Modify a action profile group, update type is MODIFY, similar with add. Only modify
     * group members are allowed, don't modify group properties, such as max size.
     */
    public static class ModifyActionProfileGroupOperator extends ActionProfileGroupOperator {
        public ModifyActionProfileGroupOperator(String nodeId, ActionProfileGroup group) {
            super(nodeId, Update.Type.MODIFY, group);
        }
    }

    public static class ReadActionProfileGroupOperator extends ActionProfileGroupOperator {
        private String actionProfileName;
        public ReadActionProfileGroupOperator(String nodeId, String actionProfileName) {
            super(nodeId);
            this.actionProfileName = actionProfileName;
        }

        public List<String> read() {
            ReadRequest.Builder requestBuilder = ReadRequest.newBuilder();
            Entity.Builder entityBuilder = Entity.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Builder groupBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.newBuilder();
            groupBuilder.setActionProfileId(actionProfileName == null ? 0 : device.getActionProfileId(actionProfileName));
            entityBuilder.setActionProfileGroup(groupBuilder);
            requestBuilder.setDeviceId(device.getDeviceId());
            requestBuilder.addEntities(entityBuilder);

            Iterator<ReadResponse> responses = device.read(requestBuilder.build());
            List<String> result = new ArrayList<>();

            while (responses.hasNext()) {
                ReadResponse response = responses.next();
                List<Entity> entityList = response.getEntitiesList();
                boolean isCompleted = response.getComplete();
                entityList.forEach(entity-> {
                    String str = device.toString(entity.getActionProfileGroup());
                    result.add(str);
                });
                if (isCompleted) break;
            }
            return result;
        }
    }
}
