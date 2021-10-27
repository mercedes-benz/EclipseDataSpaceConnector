/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class AssetToResourceTransformer implements IdsTypeTransformer<Asset, Resource> {

    public AssetToResourceTransformer() {
    }

    @Override
    public Class<Asset> getInputType() {
        return Asset.class;
    }

    @Override
    public Class<Resource> getOutputType() {
        return Resource.class;
    }

    @Override
    public @Nullable Resource transform(Asset object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        Representation result = context.transform(object, Representation.class);

        ResourceBuilder resourceBuilder = new ResourceBuilder();
        resourceBuilder._representation_(new ArrayList<>(Collections.singletonList(result)));

        return resourceBuilder.build();
    }
}
