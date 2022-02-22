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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.asset.index;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;
import javax.sql.DataSource;

public class SqlAssetIndex implements AssetIndex, DataAddressResolver {

    private final DataSource dataSource;

    public SqlAssetIndex(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public @Nullable Asset findById(String assetId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        throw new RuntimeException("Not implemented");
    }
}
