/*
 *  Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.kie.kogito.trusty.service.messaging.incoming;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.kie.kogito.tracing.decision.event.trace.TraceEvent;
import org.kie.kogito.tracing.decision.event.trace.TraceEventType;
import org.kie.kogito.trusty.service.TrustyService;
import org.kie.kogito.trusty.service.messaging.BaseEventConsumer;

@ApplicationScoped
public class TraceEventConsumer extends BaseEventConsumer<TraceEvent> {

    private TraceEventConsumer() {
        //CDI proxy
    }

    @Inject
    ObjectMapper mapper;

    @Inject
    public TraceEventConsumer(TrustyService service) {
        super(service);
    }

    @Override
    @Incoming("kogito-tracing-decision")
    public CompletionStage<Void> handleMessage(Message<String> message) {
        return super.handleMessage(message);
    }

    @Override
    protected void handleCloudEvent(CloudEvent cloudEvent) {
        final TraceEvent traceEvent;
        try {
            traceEvent = mapper.readValue(cloudEvent.getData(), TraceEvent.class);
        } catch (IOException e) {
            LOG.error("Unable to deserialize CloudEvent data as TraceEvent", e);
            return;
        }

        if (traceEvent == null) {
            LOG.error("Received CloudEvent with id {} from {} with empty data", cloudEvent.getId(), cloudEvent.getSource());
            return;
        }

        LOG.debug("Received CloudEvent with id {} from {}", cloudEvent.getId(), cloudEvent.getSource());

        TraceEventType traceEventType = traceEvent.getHeader().getType();

        if (traceEventType == TraceEventType.DMN) {
            service.processDecision(cloudEvent.getId(), traceEvent.getHeader().getResourceId().getServiceUrl(), TraceEventConverter.toDecision(traceEvent, cloudEvent.getSource().toString()));
        } else {
            LOG.error("Unsupported TraceEvent type {}", traceEventType);
        }
    }
}
