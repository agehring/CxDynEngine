/**
 * 
 */
package com.checkmarx.engine.aws;

import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * @author randy@checkmarx.com
 *
 */
@Component
public class CxAwsConfigInfo implements InfoContributor {

    private final AwsEngineConfig awsConfig;
    
    public CxAwsConfigInfo(AwsEngineConfig awsConfig) {
        super();
        this.awsConfig = awsConfig;
    }

    @Override
    public void contribute(Builder builder) {
        builder.withDetail("cx-aws-engine", awsConfig);
    }

}
