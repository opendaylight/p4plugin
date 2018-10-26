/* -*- P4_16 -*- */
#include <core.p4>
#include <v1model.p4>

/*************************************************************************
 ***********************  C O N S T A N T S  *****************************
 *************************************************************************/
 const bit<16> ETHERTYPE_IPV4 = 0x0800;
 const bit<16> ETHERTYPE_ARP  = 0x0806;
 const bit<8>  IPPROTO_ICMP   = 0x01;

const bit<16> ARP_HTYPE_ETHERNET = 0x0001;
const bit<16> ARP_PTYPE_IPV4     = 0x0800;
const bit<8>  ARP_HLEN_ETHERNET  = 6;
const bit<8>  ARP_PLEN_IPV4      = 4;
const bit<16> ARP_OPER_REQUEST   = 1;
const bit<16> ARP_OPER_REPLY     = 2;

const bit<8> ICMP_ECHO_REQUEST = 8;
const bit<8> ICMP_ECHO_REPLY   = 0;

/*************************************************************************
*********************** H E A D E R S  ***********************************
*************************************************************************/
typedef bit<48>  mac_addr_t;
typedef bit<32>  ipv4_addr_t;
typedef bit<9>   port_id_t;

header ethernet_t {
    mac_addr_t dstAddr;
    mac_addr_t srcAddr;
    bit<16>    etherType;
}

header ipv4_t {
    bit<4>       version;
    bit<4>       ihl;
    bit<8>       diffserv;
    bit<16>      totalLen;
    bit<16>      identification;
    bit<3>       flags;
    bit<13>      fragOffset;
    bit<8>       ttl;
    bit<8>       protocol;
    bit<16>      hdrChecksum;
    ipv4_addr_t  srcAddr;
    ipv4_addr_t  dstAddr;
}

header arp_t {
    bit<16> htype;
    bit<16> ptype;
    bit<8>  hlen;
    bit<8>  plen;
    bit<16> oper;
}

header arp_ipv4_t {
    mac_addr_t  sha;
    ipv4_addr_t spa;
    mac_addr_t  tha;
    ipv4_addr_t tpa;
}

header icmp_t {
    bit<8>  type;
    bit<8>  code;
    bit<16> checksum;
}

struct my_headers_t {
    ethernet_t   ethernet;
    arp_t        arp;
    arp_ipv4_t   arp_ipv4;
    ipv4_t       ipv4;
    icmp_t       icmp;
}

/*************************************************************************
 ***********************  M E T A D A T A  *******************************
 *************************************************************************/
struct my_metadata_t {
    ipv4_addr_t dst_ipv4;
    mac_addr_t  mac_da;
    mac_addr_t  mac_sa;
    port_id_t   egress_port;
    mac_addr_t  my_mac;
}

/*************************************************************************
*********************** P A R S E R  *************************************
*************************************************************************/
parser MyParser(packet_in packet,
                out my_headers_t hdr,
                inout my_metadata_t meta,
                inout standard_metadata_t standard_metadata) {

    state start {
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.etherType) {
            ETHERTYPE_IPV4 : parse_ipv4;
            ETHERTYPE_ARP  : parse_arp;
            default        : accept;
        }
    }

    state parse_arp {
        packet.extract(hdr.arp);
        transition select(hdr.arp.htype, hdr.arp.ptype,
                          hdr.arp.hlen,  hdr.arp.plen) {
            (ARP_HTYPE_ETHERNET, ARP_PTYPE_IPV4,
             ARP_HLEN_ETHERNET,  ARP_PLEN_IPV4) : parse_arp_ipv4;
            default : accept;
        }
    }

    state parse_arp_ipv4 {
        packet.extract(hdr.arp_ipv4);
        transition accept;
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition accept;
    }
}

/*************************************************************************
************   C H E C K S U M    V E R I F I C A T I O N   *************
*************************************************************************/

control MyVerifyChecksum(inout my_headers_t hdr, inout my_metadata_t meta) {
    apply {  }
}

/*************************************************************************
**************  I N G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyIngress(inout my_headers_t hdr,
                  inout my_metadata_t meta,
                  inout standard_metadata_t standard_metadata) {

    action drop() {
        mark_to_drop();
        exit;
    }

    action multicast(in bit<32> multicast_group_id, in bit<32> clone_session_id) {
        standard_metadata.mcast_grp = (bit<16>) multicast_group_id;
        clone(CloneType.I2E, clone_session_id);
    }

    table forward {
        key = {
            hdr.arp.isValid()      : exact;
            hdr.arp.oper           : ternary;
            hdr.arp_ipv4.isValid() : exact;
        }

        actions = {
            drop;
            multicast(1, 1);
        }

        const default_action = drop();
        const entries = {
            (true, ARP_OPER_REQUEST, true) : multicast(1, 1);
        }
    }
    
    apply {
        forward.apply();
    }
}

/*************************************************************************
****************  E G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyEgress(inout my_headers_t hdr,
                 inout my_metadata_t meta,
                 inout standard_metadata_t standard_metadata) {
    apply {  }
}

/*************************************************************************
*************   C H E C K S U M    C O M P U T A T I O N   **************
*************************************************************************/

control MyComputeChecksum(inout my_headers_t  hdr, inout my_metadata_t meta) {
     apply {  }
}

/*************************************************************************
***********************  D E P A R S E R  *******************************
*************************************************************************/

control MyDeparser(packet_out packet, in my_headers_t hdr) {
    apply {
        packet.emit(hdr.ethernet);
        packet.emit(hdr.arp);
        packet.emit(hdr.arp_ipv4);
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
