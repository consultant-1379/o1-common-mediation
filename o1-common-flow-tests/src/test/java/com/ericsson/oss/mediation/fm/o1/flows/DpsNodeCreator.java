
package com.ericsson.oss.mediation.fm.o1.flows;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps;

public class DpsNodeCreator {

    RuntimeConfigurableDps configurableDps;

    public DpsNodeCreator(RuntimeConfigurableDps configurableDps) {
        this.configurableDps = configurableDps;
    }

    public void createDpsMos(String nodeName) {
        createDpsMos(nodeName, true, "IDLE", true);
    }

    public void createDpsMos(String nodeName, boolean active) {
        createDpsMos(nodeName, active, "IDLE", true);
    }

    public void createDpsMos(String nodeName, boolean active, String currentSupervisionState, boolean automaticSynchronization) {
        configurableDps.withTransactionBoundaries();

        // MeContext
        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn("MeContext=" + nodeName).addAttribute("id", 1).build();

        // O1ConnectivityInformation
        HashMap<String, Object> connectivityInfoAttributes = new LinkedHashMap<>();
        connectivityInfoAttributes.put("transportProtocol", "TLS");
        connectivityInfoAttributes.put("ipAddress", "10.0.0.1");

        ManagedObject o1ConnectivityInformation =
                configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName + ",O1ConnectivityInformation=1")
                        .addAttributes(connectivityInfoAttributes)
                        .build();

        // NetworkElement
        HashMap<String, Object> networkElementAttributes = new LinkedHashMap<>();
        networkElementAttributes.put("fdn", "NetworkElement=" + nodeName);
        networkElementAttributes.put("platformType", "O1");
        networkElementAttributes.put("neType", "O1Node");
        networkElementAttributes.put("version", "1.1.0");

        ManagedObject networkElement =
                configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName).addAttributes(networkElementAttributes)
                        // NetworkElement to MeContext association
                        .withAssociation("nodeRootRef", meContext)
                        // NetworkElement to O1ConnectivityInformation association
                        .withAssociation("ciRef", o1ConnectivityInformation)
                        .build();

        // MeContext to NetworkElement association
        meContext.addAssociation("networkElementRef", networkElement);

        // FmAlarmSupervision
        HashMap<String, Object> fmAlarmSupervisionAttributes = new LinkedHashMap<>();
        fmAlarmSupervisionAttributes.put("active", active);
        fmAlarmSupervisionAttributes.put("heartbeatinterval", 100);
        fmAlarmSupervisionAttributes.put("heartbeatTimeout", 300);
        fmAlarmSupervisionAttributes.put("automaticSynchronization", automaticSynchronization);

        ManagedObject fmAlarmSupervision = configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName + ",FmAlarmSupervision=1")
                .addAttributes(fmAlarmSupervisionAttributes)
                .build();
        // target for FmAlarmSupervision is the NetworkElement. The NetworkElement must have association 'ciRef' to O1ConnectivityInformation to allow
        // the mediation service to extract the ipAddress of the node by linking the FmAlarmSupervision -> NetworkElement (target) ->
        // O1ConnectivityInformation (ciRef).
        fmAlarmSupervision.setTarget(networkElement);
        // Same for NetworkElement
        networkElement.setTarget(networkElement);

        // FmFunction
        HashMap<String, Object> fmFunctionAttributes = new LinkedHashMap<>();
        fmFunctionAttributes.put("FmFunctionId", 1);
        fmFunctionAttributes.put("alarmSuppressedState", false);
        // currentServiceState value Range: IN_SERVICE, HEART_BEAT_FAILURE, NODE_SUSPENDED, SYNCHRONIZATION, SYNC_ONGOING, IDLE, ALARM_SUPPRESSED,
        // TECHNICIAN_PRESENT, OUT_OF_SYNC
        fmFunctionAttributes.put("currentServiceState", currentSupervisionState);
        // subscriptionState value range: DISABLED, ENABLED, ENABLING, DISABLING
        fmFunctionAttributes.put("subscriptionState", "DISABLED");
        configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName + ",FmFunction=1")
                .addAttributes(fmFunctionAttributes)
                .build();
    }
}
