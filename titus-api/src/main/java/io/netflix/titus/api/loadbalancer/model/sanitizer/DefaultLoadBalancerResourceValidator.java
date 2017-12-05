/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.netflix.titus.api.loadbalancer.model.sanitizer;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.netflix.titus.api.connector.cloud.LoadBalancerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultLoadBalancerResourceValidator implements LoadBalancerResourceValidator {
    private static final Logger logger = LoggerFactory.getLogger(DefaultLoadBalancerJobValidator.class);
    private static final int VALIDATION_TIMEOUT_MIN = 1;

    private final LoadBalancerConnector loadBalancerConnector;

    @Inject
    public DefaultLoadBalancerResourceValidator(LoadBalancerConnector connector) {
        this.loadBalancerConnector = connector;
    }

    @Override
    public void validateLoadBalancer(String loadBalancerId) throws Exception {
        // We depend on the load balancer implementation-specific connector to indicate
        // what makes a load balancer ID valid or not.
        loadBalancerConnector.isValid(loadBalancerId).await(VALIDATION_TIMEOUT_MIN, TimeUnit.MINUTES);
    }
}
