/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Converter {
    private String value;
    private Type type;
    public Converter(String value, Type type) {
        this.value = value;
        this.type = type;
    }

    public byte[] toBytes() {
        byte[] result;
        switch (type) {
            case IP: {
                result =  new IpAddressConverter().toBytes();
                break;
            }

            case MAC: {
                result = new MacAddressConverter().toBytes();
                break;
            }

            case NUMBER:{
                result = new NumberConverter().toBytes();
                break;
            }
            default:throw new IllegalArgumentException("Invalid value type.");
        }
        return result;
    }

    public enum Type {
        IP,
        MAC,
        NUMBER,
    }

    /**
     * Converts an ip address to a byte array.
     */
    private class IpAddressConverter {
        private byte[] toBytes() {
            InetAddress address;
            try {
                address = InetAddress.getByName(value);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid ip address = " + value + ".");
            }
            return address.getAddress();
        }
    }

    /**
     * Converts a mac address to a byte array with a length of 6;
     */
    private class MacAddressConverter {
        private byte[] toBytes() {
            String[] strArray;
            byte[] byteArray;
            /* mac address, similar with aa:bb:cc:dd:ee:ff, 1:2:3:4:5:6 */
            if (value.matches("([0-9a-fA-F]{1,2}:){5}[0-9a-fA-F]{1,2}")) {
                strArray = value.split(":");
                byteArray = new byte[strArray.length];
                for (int i = 0; i < strArray.length; i++) {
                    byteArray[i] = (byte) Integer.parseInt(strArray[i], 16);
                }
            } else {
                throw new IllegalArgumentException("Invalid mac address = " + value + ".");
            }
            return byteArray;
        }
    }

    /**
     * Converts  a number to a byte array with a length of 4;
     */
    private class NumberConverter {
        private byte[] toBytes() {
            byte[] byteArray = new byte[4];
            int var = Integer.parseInt(value);
            for (int i = 0; i < 4; i++) {
                byteArray[i] = (byte) (var >> ((4 - i - 1) * 8) & 0xFF);
            }
            return byteArray;
        }
    }
}
