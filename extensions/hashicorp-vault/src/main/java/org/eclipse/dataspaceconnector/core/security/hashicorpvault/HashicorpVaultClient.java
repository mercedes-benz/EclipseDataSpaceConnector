/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

class HashicorpVaultClient {
    static final String VAULT_DATA_ENTRY_NAME = "content";
    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    private static final String VAULT_REQUEST_HEADER = "X-Vault-Request";
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json");
    private static final String VAULT_API_VERSION = "v1";
    private static final String VAULT_SECRET_PATH = "secret";
    private static final String VAULT_SECRET_DATA_PATH = "data";
    private static final String VAULT_SECRET_METADATA_PATH = "metadata";
    private static final String CALL_UNSUCCESSFUL_ERROR_TEMPLATE = "[Hashicorp Vault] Call unsuccessful: %s";
    @NotNull
    private final HashicorpVaultClientConfig config;
    @NotNull
    private final OkHttpClient okHttpClient;
    @NotNull
    private final TypeManager typeManager;

    public HashicorpVaultClient(@NotNull HashicorpVaultClientConfig config, @NotNull OkHttpClient okHttpClient, @NotNull TypeManager typeManager) {
        this.config = config;
        this.okHttpClient = okHttpClient;
        this.typeManager = typeManager;
    }

    Result<String> getSecretValue(@NotNull String key) {
        var requestURI = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var headers = getHeaders();
        var request = new Request.Builder().url(requestURI).headers(headers).get().build();

        try (var response = okHttpClient.newCall(request).execute()) {

            if (response.isSuccessful()) {
                if (response.code() == 404) {
                    return Result.failure(
                            String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, "Secret not found"));
                }

                var responseBody = Objects.requireNonNull(response.body()).string();
                var payload = typeManager.readValue(responseBody, HashicorpVaultGetEntryResponsePayload.class);
                var value = Objects.requireNonNull(payload.getData().getData().get(VAULT_DATA_ENTRY_NAME));

                return Result.success(value);
            } else {
                return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
            }

        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    Result<HashicorpVaultCreateEntryResponsePayload> setSecret(
            @NotNull String key, @NotNull String value) {
        var requestURI = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var headers = getHeaders();
        var requestPayload =
                HashicorpVaultCreateEntryRequestPayload.builder()
                        .data(Collections.singletonMap(VAULT_DATA_ENTRY_NAME, value))
                        .build();
        var request =
                new Request.Builder()
                        .url(requestURI)
                        .headers(headers)
                        .post(createRequestBody(requestPayload))
                        .build();

        try (var response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                var responseBody = Objects.requireNonNull(response.body()).string();
                var responsePayload =
                        typeManager.readValue(responseBody, HashicorpVaultCreateEntryResponsePayload.class);
                return Result.success(responsePayload);
            } else {
                return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
            }
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    Result<Void> destroySecret(@NotNull String key) {
        var requestURI = getSecretUrl(key, VAULT_SECRET_METADATA_PATH);
        var headers = getHeaders();
        var request = new Request.Builder().url(requestURI).headers(headers).delete().build();

        try (var response = okHttpClient.newCall(request).execute()) {
            return response.isSuccessful() || response.code() == 404
                    ? Result.success()
                    : Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    @NotNull
    private Headers getHeaders() {
        var headersBuilder =
                new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
        if (config.getVaultToken() != null) {
            headersBuilder = headersBuilder.add(VAULT_TOKEN_HEADER, config.getVaultToken());
        }
        return headersBuilder.build();
    }

    private String getBaseUrl() {
        var baseUrl = config.getVaultUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }

    private String getSecretUrl(String key, String entryType) {

        key = URLEncoder.encode(key, StandardCharsets.UTF_8);
        return URI.create(
                        String.format(
                                "%s/%s/%s/%s/%s",
                                getBaseUrl(), VAULT_API_VERSION, VAULT_SECRET_PATH, entryType, key))
                .toString();
    }

    private RequestBody createRequestBody(Object requestPayload) {
        var jsonRepresentation = typeManager.writeValueAsString(requestPayload);
        return RequestBody.create(jsonRepresentation, MEDIA_TYPE_APPLICATION_JSON);
    }
}