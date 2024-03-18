/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.listener;

import org.eclipse.edc.connector.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessDeprovisioned;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessDeprovisioningRequested;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessProvisioned;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessProvisioningRequested;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessSuspended;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;

import java.time.Clock;

/**
 * Listener responsible for creating and publishing events regarding TransferProcess state changes
 */
public class TransferProcessEventListener implements TransferProcessListener {
    private final EventRouter eventRouter;
    private final Clock clock;

    public TransferProcessEventListener(EventRouter eventRouter, Clock clock) {
        this.eventRouter = eventRouter;
        this.clock = clock;
    }

    @Override
    public void initiated(TransferProcess process) {
        var event = withBaseProperties(TransferProcessInitiated.Builder.newInstance(), process)
                .build();

        publish(event);
    }

    @Override
    public void provisioningRequested(TransferProcess process) {
        var event = withBaseProperties(TransferProcessProvisioningRequested.Builder.newInstance(), process)
                .build();

        publish(event);
    }

    @Override
    public void provisioned(TransferProcess process) {
        var event = withBaseProperties(TransferProcessProvisioned.Builder.newInstance(), process)
                .build();

        publish(event);
    }

    @Override
    public void requested(TransferProcess process) {
        var event = withBaseProperties(TransferProcessRequested.Builder.newInstance(), process)
                .build();

        publish(event);
    }

    @Override
    public void started(TransferProcess process, TransferProcessStartedData additionalData) {
        var event = withBaseProperties(TransferProcessStarted.Builder.newInstance(), process)
                .dataAddress(additionalData.getDataAddress())
                .build();

        publish(event);
    }

    @Override
    public void completed(TransferProcess process) {
        var event = withBaseProperties(TransferProcessCompleted.Builder.newInstance(), process)
                .build();

        publish(event);
    }

    @Override
    public void suspended(TransferProcess process) {
        var event = TransferProcessSuspended.Builder.newInstance()
                .reason(process.getErrorDetail())
                .transferProcessId(process.getId())
                .callbackAddresses(process.getCallbackAddresses())
                .build();

        publish(event);
    }

    @Override
    public void terminated(TransferProcess process) {
        var event = withBaseProperties(TransferProcessTerminated.Builder.newInstance(), process)
                .reason(process.getErrorDetail())
                .build();

        publish(event);
    }

    @Override
    public void deprovisioningRequested(TransferProcess process) {
        var event = withBaseProperties(TransferProcessDeprovisioningRequested.Builder.newInstance(), process)
                .build();

        publish(event);
    }

    @Override
    public void deprovisioned(TransferProcess process) {
        var event = withBaseProperties(TransferProcessDeprovisioned.Builder.newInstance(), process)
                .build();

        publish(event);
    }

    private <T extends TransferProcessEvent, B extends TransferProcessEvent.Builder<T, B>> B withBaseProperties(B builder, TransferProcess process) {
        return builder.transferProcessId(process.getId())
                .contractId(process.getContractId())
                .assetId(process.getAssetId())
                .type(process.getType().name())
                .callbackAddresses(process.getCallbackAddresses());
    }

    @SuppressWarnings("unchecked")
    private void publish(TransferProcessEvent event) {
        var envelope = EventEnvelope.Builder.newInstance()
                .payload(event)
                .at(clock.millis())
                .build();

        eventRouter.publish(envelope);
    }
}
