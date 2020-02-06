/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.rollup.rest;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.core.rollup.RollupField;
import org.elasticsearch.xpack.core.rollup.action.StopRollupJobAction;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.elasticsearch.rest.RestRequest.Method.POST;

public class RestStopRollupJobAction extends BaseRestHandler {

    @Override
    public List<Route> handledRoutes() {
        return singletonList(new Route("/_rollup/job/{id}/_stop", POST));
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) {
        String id = restRequest.param(RollupField.ID.getPreferredName());
        TimeValue timeout = restRequest.paramAsTime(StopRollupJobAction.TIMEOUT.getPreferredName(), StopRollupJobAction.DEFAULT_TIMEOUT);
        boolean waitForCompletion = restRequest.paramAsBoolean(StopRollupJobAction.WAIT_FOR_COMPLETION.getPreferredName(), false);
        StopRollupJobAction.Request request = new StopRollupJobAction.Request(id, waitForCompletion, timeout);

        return channel -> client.execute(StopRollupJobAction.INSTANCE, request, new RestToXContentListener<>(channel));
    }

    @Override
    public String getName() {
        return "stop_rollup_job";
    }

}
