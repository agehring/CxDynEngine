/**
 * 
 */
package com.checkmarx.engine.spring;

import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import com.checkmarx.engine.CxConfig;
import com.checkmarx.engine.domain.EnginePoolConfig;

/**
 * @author randy@thegeyers.com
 *
 */
@Component
public class CxConfigInfo implements InfoContributor {
    
    private final CxConfig config;
    private final EnginePoolConfig poolConfig;

    public CxConfigInfo(CxConfig config, EnginePoolConfig poolConfig) {
        super();
        this.config = config;
        this.poolConfig = poolConfig;
    }

    @Override
    public void contribute(Builder builder) {
        builder.withDetail("cxConfig", this.config);
        builder.withDetail("cxPoolConfig", this.poolConfig);
    }

}
