/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_DATASET;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER_ID;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID;


/**
 * Creates a {@link JsonObject} from a {@link ContractRequestMessage}.
 */
public class JsonObjectFromContractRequestMessageTransformer extends AbstractJsonLdTransformer<ContractRequestMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractRequestMessageTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractRequestMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractRequestMessage requestMessage, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, requestMessage.getId())
                .add(TYPE, DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE)
                .add(DSPACE_PROPERTY_CONSUMER_PID, requestMessage.getConsumerPid())
                .add(DSPACE_PROPERTY_PROCESS_ID, requestMessage.getProcessId());

        addIfNotNull(requestMessage.getProviderPid(), DSPACE_PROPERTY_PROVIDER_PID, builder);
        addIfNotNull(requestMessage.getCallbackAddress(), DSPACE_PROPERTY_CALLBACK_ADDRESS, builder);

        if (requestMessage.getContractOffer() != null) {
            builder.add(DSPACE_PROPERTY_DATASET, requestMessage.getContractOffer().getAssetId());
            var policy = context.transform(requestMessage.getContractOffer().getPolicy(), JsonObject.class);
            if (policy == null) {
                context.problem()
                        .nullProperty()
                        .type(ContractRequestMessage.class)
                        .property("contractOffer")
                        .report();
                return null;
            }

            var enrichedPolicy = Json.createObjectBuilder(policy)
                    .add(ID, requestMessage.getContractOffer().getId())
                    .build();
            builder.add(DSPACE_PROPERTY_OFFER, enrichedPolicy);

        } else {
            builder.add(DSPACE_PROPERTY_OFFER_ID, requestMessage.getContractOfferId());
            addIfNotNull(requestMessage.getDataset(), DSPACE_PROPERTY_DATASET, builder);
        }

        return builder.build();
    }

}
