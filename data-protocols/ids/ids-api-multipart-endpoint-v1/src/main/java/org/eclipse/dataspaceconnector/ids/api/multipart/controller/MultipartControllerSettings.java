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

package org.eclipse.dataspaceconnector.ids.api.multipart.controller;

import org.jetbrains.annotations.Nullable;

public class MultipartControllerSettings {
    private final String id;

    private MultipartControllerSettings(@Nullable String id) {
        this.id = id;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public static class Builder {
        private String id;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        public MultipartControllerSettings build() {
            return new MultipartControllerSettings(id);
        }
    }
}
