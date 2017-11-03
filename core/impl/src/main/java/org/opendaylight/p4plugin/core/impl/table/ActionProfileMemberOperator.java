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
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.ActionProfileMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.MemberKey;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ActionProfileMemberOperator {
    protected P4Device device;
    protected Update.Type type;
    protected org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member;

    private ActionProfileMemberOperator(String nodeId) {
        this.device = DeviceManager.getInstance().findConfiguredDevice(nodeId);
    }

    private ActionProfileMemberOperator(String nodeId, Update.Type type) {
        this(nodeId);
        this.type = type;
    }

    /**
     * This constructor is used to add and modify a member.
     */
    private ActionProfileMemberOperator(String nodeId, Update.Type type, ActionProfileMember member) {
        this(nodeId, type);
        this.member = device.toMessage(member);
    }

    /**
     * This constructor is used to delete a member.
     */
    private ActionProfileMemberOperator(String nodeId, Update.Type type, MemberKey member) {
        this(nodeId, type);
        this.member = device.toMessage(member);
    }

    /**
     * Write a action profile member actually.
     */
    public boolean operate() {
        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        Update.Builder updateBuilder = Update.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileMember(member);
        updateBuilder.setType(type);
        updateBuilder.setEntity(entityBuilder);
        requestBuilder.addUpdates(updateBuilder);
        requestBuilder.setDeviceId(device.getDeviceId());
        return device.write(requestBuilder.build()) != null;
    }

    /**
     * Add a action profile member, update type is INSERT.
     */
    public static class AddActionProfileMemberOperator extends ActionProfileMemberOperator {
        public AddActionProfileMemberOperator(String nodeId, ActionProfileMember member) {
            super(nodeId, Update.Type.INSERT, member);
        }
    }

    /**
     * Delete a action profile member, update type is DELETE. Only action profile name and
     * member id are required to delete a member.
     */
    public static class DeleteActionProfileMemberOperator extends ActionProfileMemberOperator {
        public DeleteActionProfileMemberOperator(String nodeId, MemberKey member) {
            super(nodeId, Update.Type.DELETE, member);
        }
    }

    /**
     * Modify a action profile member, update type is MODIFY, similar with add.
     */
    public static class ModifyActionProfileMemberOperator extends ActionProfileMemberOperator{
        public ModifyActionProfileMemberOperator(String nodeId, ActionProfileMember member) {
            super(nodeId, Update.Type.MODIFY, member);
        }
    }

    /**
     * Read all members of a action profile.
     */
    public static class ReadActionProfileMemberOperator extends ActionProfileMemberOperator {
        private String actionProfileName;
        public ReadActionProfileMemberOperator(String nodeId, String actionProfileName) {
            super(nodeId);
            this.actionProfileName = actionProfileName;
        }

        public List<String> read() {
            ReadRequest.Builder requestBuilder = ReadRequest.newBuilder();
            Entity.Builder entityBuilder = Entity.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.Builder memberBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.newBuilder();

            memberBuilder.setActionProfileId(actionProfileName == null ? 0 : device.getActionProfileId(actionProfileName));
            entityBuilder.setActionProfileMember(memberBuilder);
            requestBuilder.setDeviceId(device.getDeviceId());
            requestBuilder.addEntities(entityBuilder);

            Iterator<ReadResponse> responses = device.read(requestBuilder.build());
            List<String> result = new ArrayList<>();

            while (responses.hasNext()) {
                ReadResponse response = responses.next();
                List<Entity> entityList = response.getEntitiesList();
                boolean isCompleted = response.getComplete();
                entityList.forEach(entity-> {
                    String str = device.toString(entity.getActionProfileMember());
                    result.add(str);
                });
                if (isCompleted) break;
            }
            return result;
        }
    }
}
