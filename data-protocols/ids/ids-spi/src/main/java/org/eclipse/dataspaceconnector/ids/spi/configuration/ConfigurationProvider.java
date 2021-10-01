package org.eclipse.dataspaceconnector.ids.spi.configuration;

import java.net.URI;
import java.util.Optional;

/**
 * Provider for the IDS configuration.
 */
public interface ConfigurationProvider {
    Optional<URI> resolveId();

    Optional<String> resolveTitle();

    Optional<String> resolveDescription();

    Optional<URI> resolveMaintainer();

    Optional<URI> resolveCurator();

    Optional<URI> resolveConnectorEndpoint();
}
