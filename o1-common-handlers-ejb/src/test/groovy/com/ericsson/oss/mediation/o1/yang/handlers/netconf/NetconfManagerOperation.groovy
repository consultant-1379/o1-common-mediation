package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationConnection
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager

interface NetconfManagerOperation extends NetconfManager, NetconfOperationConnection {

}