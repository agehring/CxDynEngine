/**
 * 
 */
package com.checkmarx.engine.azure;

import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * @author ken.mcdonald@checkmarx.com
 *
 */
@Profile("azure")
@Component
public class CxAzureConfigInfo implements InfoContributor {

    private final AzureEngineConfig azureConfig;
    
    public CxAzureConfigInfo(AzureEngineConfig azureConfig) {
        super();
        this.azureConfig = azureConfig;
    }

    @Override
    public void contribute(Builder builder) {
        builder.withDetail("cx-azure-engine", azureConfig);
    }

}
