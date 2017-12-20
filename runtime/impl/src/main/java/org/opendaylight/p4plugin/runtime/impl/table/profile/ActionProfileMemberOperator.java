/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.table.profile;

import org.opendaylight.p4plugin.runtime.impl.device.DeviceManager;
import org.opendaylight.p4plugin.runtime.impl.device.P4Device;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.ActionProfileMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.MemberKey;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActionProfileMemberOperator {
    private String nodeId;

    public ActionProfileMemberOperator(String nodeId) {
        this.nodeId = nodeId;
    }

    private P4Device getDevice(String nodeId) {
        return DeviceManager.getInstance().findConfiguredDevice(nodeId);
    }

    public boolean add(ActionProfileMember inputMember) {
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member =
                getDevice(nodeId).toMessage(inputMember);
        return operate(member, Update.Type.INSERT);
    }

    public boolean modify(ActionProfileMember inputMember) {
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member =
                getDevice(nodeId).toMessage(inputMember);
        return operate(member, Update.Type.MODIFY);
    }

    public boolean delete(MemberKey key) {
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member =
                getDevice(nodeId).toMessage(key);
        return operate(member, Update.Type.DELETE);
    }

    public List<String> read(String actionProfileName) {
        P4Device device = getDevice(nodeId);
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

    private boolean operate(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member, Update.Type type) {
        P4Device device = getDevice(nodeId);
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
}
