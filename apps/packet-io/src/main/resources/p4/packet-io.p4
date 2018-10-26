/* -*- P4_16 -*- */
#include <core.p4>
#include <v1model.p4>

#define CPU_PORT 64
typedef bit<48> macAddr_t;

/*************************************************************************
*********************** H E A D E R S  ***********************************
*************************************************************************/

@controller_header("packet_in")
header packet_in_header_t {
    bit<9> ingress_port;
    bit<16> reason;
}

@controller_header("packet_out")
header packet_out_header_t {
    bit<64> cpu_preamble;
    bit<9> egress_port;
}

struct metadata {
    /* empty */
}

header ethernet_t {dd
    macAddr_t dstAddr;
    macAddr_t srcAddr;
    bit<16>   etherType;
}

struct headers {
   packet_out_header_t my_packet_out;
   packet_in_header_t my_packet_in;
   ethernet_t ethernet;
}

enum bit<16> packet_in_reason {
    arp = 0xFF
}

/*************************************************************************
*********************** P A R S E R  ***********************************
*************************************************************************/

parser MyParser(packet_in packet,
                out headers hdr,
                inout metadata meta,
                inout standard_metadata_t standard_metadata) {

    state start {
        transition select(packet.lookahead<packet_out_header_t>().cpu_preamble) {
            64w0x0 : parse_my_packet_out;
            default : parse_ethernet;
        }
    }

    state parse_my_packet_out {
        packet.extract(hdr.my_packet_out);
        transition accept;
    }

    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition accept;
    }
}

/*************************************************************************
************   C H E C K S U M    V E R I F I C A T I O N   *************
*************************************************************************/

control MyVerifyChecksum(inout headers hdr, inout metadata meta) {
    apply {  }
}

/*************************************************************************
**************  I N G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyIngress(inout headers hdr,
                  inout metadata meta,
                  inout standard_metadata_t standard_metadata) {

    action send_packet_in() {
        standard_metadata.egress_spec = CPU_PORT;
        hdr.my_packet_in.setValid();
        hdr.my_packet_in.ingress_port = standard_metadata.ingress_port;
        hdr.my_packet_in.reason = (bit<16>)packet_in_reason.arp;
    }

    action drop() {
        mark_to_drop();
    }

    action send_packet_out() {
        standard_metadata.egress_spec = hdr.my_packet_out.egress_port;
        hdr.my_packet_out.setInvalid();
    }

    table t_upsend {
        key = {
            hdr.ethernet.etherType : exact;
        }

        actions = {
            send_packet_in;
            drop;
        }

        size = 1;
        default_action = drop();
        const entries = {
            16w0x0806 : send_packet_in();
        }
    }

    apply {
        if(hdr.my_packet_out.isValid()) {
            send_packet_out();
        } else {
            if(hdr.ethernet.isValid()) {
                t_upsend.apply();
            }
        }
    }
}

/*************************************************************************
****************  E G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyEgress(inout headers hdr,
                 inout metadata meta,
                 inout standard_metadata_t standard_metadata) {
    apply {  }
}

/*************************************************************************
*************   C H E C K S U M    C O M P U T A T I O N   **************
*************************************************************************/

control MyComputeChecksum(inout headers  hdr, inout metadata meta) {
     apply {  }
}

/*************************************************************************
***********************  D E P A R S E R  *******************************
*************************************************************************/

control MyDeparser(packet_out packet, in headers hdr) {
    apply {
        packet.emit(hdr.my_packet_in);
        packet.emit(hdr.ethernet);
    }
}

/*************************************************************************
***********************  S W I T C H  ************************************
*************************************************************************/

V1Switch(
    MyParser(),
    MyVerifyChecksum(),
    MyIngress(),
    MyEgress(),
    MyComputeChecksum(),
    MyDeparser()
) main;