/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.p4plugin.channel;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.MatchField;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4info.proto.Preamble;
import org.opendaylight.p4plugin.p4info.proto.Table;
import org.opendaylight.p4plugin.p4runtime.proto.ForwardingPipelineConfig;
import org.opendaylight.p4plugin.p4runtime.proto.P4RuntimeGrpc;
import org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest;
import org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigResponse;
import org.opendaylight.p4plugin.p4runtime.proto.FieldMatch;
import org.opendaylight.p4plugin.p4runtime.proto.TableAction;
import org.opendaylight.p4plugin.p4runtime.proto.TableEntry;
import org.opendaylight.p4plugin.p4runtime.proto.Entity;
import org.opendaylight.p4plugin.p4runtime.proto.Update;
import org.opendaylight.p4plugin.p4runtime.proto.WriteRequest;
import java.io.FileInputStream;
import java.io.FileReader;
import org.opendaylight.p4plugin.p4runtime.proto.WriteResponse;

import java.io.IOException;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.EnumMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChannelImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelImpl.class.getName());

    private final ManagedChannel channel;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private P4Info info;
    public ChannelImpl(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    ChannelImpl(ManagedChannelBuilder<?> channelBuilder) {
          channel = channelBuilder.build();
          blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        //channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void setForwardingPipelineConfig() {
        P4Info.Builder p4InfoBuilder = P4Info.newBuilder();

        try {
            TextFormat.merge(new FileReader("simple_router.proto.txt"), p4InfoBuilder);
        } catch (IOException e) {
            LOG.info("simple_router.proto.txt file not found");
        }

        info = p4InfoBuilder.build();
        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        configBuilder.setDeviceId(0);
        configBuilder.setP4Info(p4InfoBuilder.build());

        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();

        try {
            p4DeviceConfigBuilder.setDeviceData(ByteString.readFrom(new FileInputStream("simple_router.json")));
        } catch (IOException e) {
            LOG.info("simple_router.json file not found");
        }

        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());

        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                                                     .setAction(org.opendaylight.p4plugin.p4runtime.proto
                                                                .SetForwardingPipelineConfigRequest.Action
                                                                .VERIFY_AND_COMMIT)
                                                     .addConfigs(configBuilder.build())
                                                     .build();
        SetForwardingPipelineConfigResponse response;

        try {
            response = blockingStub.setForwardingPipelineConfig(request);
        } catch (StatusRuntimeException e) {
            LOG.info("RPC failed: {}", e.getStatus());
        }
    }

    public void writeTableEntry(WriteTableEntryData data) {
        WriteRequest.Builder request = WriteRequest.newBuilder();
        request.setDeviceId(data.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.valueOf(data.getUpdateType().toString()));

        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(data.toMessage(info));
        updateBuilder.setEntity(entityBuilder.build());
        request.addUpdates(updateBuilder.build());

        WriteResponse response = null;
        try {
            response = blockingStub.write(request.build());
        } catch (StatusRuntimeException e) {
            LOG.info("RPC failed: {}", e.getStatus());
        }
    }

    /**
     *
     //* @param description must be a String like below, every segment must flow a backspace except the last one.Similarly
     *                    to bmv2 CLI cmd: write insert ipv4.lpm ipv4.dstAddr lpm set_nhop 10.0.0.0/24 => 10.0.0.10 1
     *                     "RPC_ACTION:write " +     //write, read
     *                     "UPDATE_TYPE:insert " +   //insert,modify, delete
     *                     "TABLE_NAME:ipv4_lpm " +
     *                     "FIELD_MATCH_NAME:ipv4.dstAddr " +
     *                     "FIELD_MATCH_TYPE:lpm " + //lpm, exact, ternary
     *                     "ENTRY_ACTION:set_nhop " +
     *                     "FIELD_MATCH_VALUE:10.0.0.0/24 " +
     *                     "PARAMS:nhop_ipv4 = 10.0.0.10, port = 1"
     * @return An EnumMap
     */
    /*
     public Map<ENTRY_DESCRIPTION, String> parseEntryDescription(String description) {
        Map<ENTRY_DESCRIPTION, String> enumMap = new EnumMap(ENTRY_DESCRIPTION.class);
        String pattern = "^(RPC_ACTION\\s*:\\s*write|read)\\s+"
                       + "(UPDATE_TYPE\\s*:\\s*insert|modify|delete)\\s+"
                       + "(TABLE_NAME\\s*:\\s*\\w+)\\s+"
                       + "(FIELD_MATCH_NAME\\s*:\\s*[\\w.]+)\\s+"
                       + "(FIELD_MATCH_TYPE\\s*:\\s*lpm|exact|tenary)\\s+"
                       + "(ENTRY_ACTION\\s*:\\w+)\\s+"
                       + "(FIELD_MATCH_VALUE\\s*:\\s*[\\w:_./]+)\\s+"
                       + "(PARAMS\\s*:\\s*[\\w:_./= ]+,?[\\s\\w:_./= ,]*)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(description);
        if(m.find()) {
            for (int i = 1; i < m.groupCount() + 1; i++) {
                String[] keyValue = m.group(i).replace(" ","").replace("\t","").split(":");
                enumMap.put(ENTRY_DESCRIPTION.valueOf(keyValue[0]), keyValue[1]);
            }
        }
        return enumMap;
    }

    public Map<String, String> parseParams(String paramDescription) {
        String pattern = "(\\w+=[\\d.:]+),?(\\w+=[\\d.:])?,?(\\w+=[\\d.:]*)?";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(paramDescription);
        Map<String, String> paramsMap = new LinkedHashMap<>();
        if (m.find()) {
            for (int i = 1; i < m.groupCount() + 1; i++) {
                if (m.group(i) != null) { paramsMap.put(m.group(i).split("=")[0],m.group(i).split("=")[1]); }
            }
        }
        return paramsMap;
    }


    public void writeTableEntry(String entryDescription) {
        Map<ENTRY_DESCRIPTION, String> enumMap = parseEntryDescription(entryDescription);
        Map<String, String> paramsMap = parseParams(enumMap.get(ENTRY_DESCRIPTION.PARAMS));

        int tableId = getTableId(enumMap.get(ENTRY_DESCRIPTION.TABLE_NAME));
        int matchFieldId = getMatchFieldId(enumMap.get(ENTRY_DESCRIPTION.TABLE_NAME),
                                           enumMap.get(ENTRY_DESCRIPTION.FIELD_MATCH_NAME));
        int actionId = getActionId(enumMap.get(ENTRY_DESCRIPTION.ENTRY_ACTION));
        org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder =
                                                          org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();
        actionBuilder.setActionId(actionId);

        for (String k : paramsMap.keySet()) {
            org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder =
                                                    org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder();
            paramBuilder.setParamId(getParamId(enumMap.get(ENTRY_DESCRIPTION.ENTRY_ACTION), k));
            String[] paramValueStrArray = null;
            byte[] paramValueByteArry = null;
            if (paramsMap.get(k).contains(".")) {
                paramValueStrArray = paramsMap.get(k).split("\\.");
                paramValueByteArry = new byte[paramValueStrArray.length];
                for (int i = 0; i < paramValueStrArray.length; i++) {
                    paramValueByteArry[i] = (byte)Integer.parseInt(paramValueStrArray[i]);
                }
            }else if (paramsMap.get(k).contains(":")) {
                paramValueStrArray = paramsMap.get(k).split(":");
                paramValueByteArry = new byte[paramValueStrArray.length];
                for (int i = 0; i < paramValueStrArray.length; i++) {
                    paramValueByteArry[i] = (byte)Integer.parseInt(paramValueStrArray[i],16);
                }
            }else {
                //Integer.parseInt()
            }

            paramBuilder.setValue(ByteString.copyFrom(paramValueByteArry));
            actionBuilder.addParams(paramBuilder.build());
        }

        TableAction.Builder tableActionBuilder = TableAction.newBuilder();
        tableActionBuilder.setAction(actionBuilder.build());

        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        fieldMatchBuilder.setFieldId(matchFieldId);

        if(enumMap.get(ENTRY_DESCRIPTION.FIELD_MATCH_TYPE).equals("lpm")) {
            String[] valueArray = enumMap.get(ENTRY_DESCRIPTION.FIELD_MATCH_VALUE).split("/");
            String[] addrStrArray = valueArray[0].split(".");
            byte[] addrIntArry = new byte[4];
            for (int i = 0; i < addrStrArray.length; i++) { addrIntArry[i] = (byte)Integer.parseInt(addrStrArray[i]); }
            FieldMatch.LPM.Builder lpmBuilder = FieldMatch.LPM.newBuilder();
            lpmBuilder.setValue(ByteString.copyFrom(addrIntArry));
            lpmBuilder.setPrefixLen(Integer.parseInt(valueArray[1]));
            fieldMatchBuilder.setLpm(lpmBuilder.build());
        }

        if (enumMap.get(ENTRY_DESCRIPTION.FIELD_MATCH_TYPE).equals("exact")) {
            String[] valueArray = enumMap.get(ENTRY_DESCRIPTION.FIELD_MATCH_VALUE).split(":");
            byte[] addrByteArry = new byte[valueArray.length];
            for (int i = 0; i < valueArray.length; i++) { addrByteArry[i] = (byte)Integer.parseInt(valueArray[i]); }
            FieldMatch.Exact.Builder exactBuilder = FieldMatch.Exact.newBuilder();
            exactBuilder.setValue(ByteString.copyFrom(addrByteArry));
            fieldMatchBuilder.setExact(exactBuilder.build());
        }

        TableEntry.Builder tableEntryBuilder = TableEntry.newBuilder();
        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.addMatch(fieldMatchBuilder.build());
        tableEntryBuilder.setAction(tableActionBuilder.build());

        WriteRequest.Builder request = WriteRequest.newBuilder();
        request.setDeviceId(0);
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.valueOf(enumMap.get(ENTRY_DESCRIPTION.RPC_ACTION).toUpperCase()));

        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(tableEntryBuilder.build());
        updateBuilder.setEntity(entityBuilder.build());
        request.addUpdates(updateBuilder.build());

        WriteResponse response = null;

        try {
            response = blockingStub.write(request.build());
        } catch (StatusRuntimeException e) {
            LOG.info("RPC failed: {}", e.getStatus());
        }
    }
*/

}
