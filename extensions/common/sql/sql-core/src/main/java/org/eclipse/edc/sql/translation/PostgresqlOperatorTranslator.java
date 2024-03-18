/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.sql.translation;

import java.util.Collection;

import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.CONTAINS;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.EQUAL;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.IN;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.LIKE;

/**
 * Postgresql's implementation of the operator translator
 */
public class PostgresqlOperatorTranslator implements SqlOperatorTranslator {

    @Override
    public SqlOperator translate(String operator) {
        return switch (operator) {
            case EQUAL -> new SqlOperator("=", Object.class);
            case LIKE -> new SqlOperator("like", String.class);
            case IN -> new SqlOperator("in", Collection.class);
            case CONTAINS -> new SqlOperator("??", Object.class);
            default -> null;
        };
    }
}
