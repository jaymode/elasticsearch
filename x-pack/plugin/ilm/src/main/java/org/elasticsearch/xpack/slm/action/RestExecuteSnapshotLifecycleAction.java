/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.slm.action;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.core.slm.action.ExecuteSnapshotLifecycleAction;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class RestExecuteSnapshotLifecycleAction extends BaseRestHandler {

    @Override
    public List<Route> handledRoutes() {
        return unmodifiableList(asList(
            new Route("/_slm/policy/{name}/_execute", POST),
            new Route("/_slm/policy/{name}/_execute", PUT)));
    }

    @Override
    public String getName() {
        return "slm_execute_lifecycle";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        String snapLifecycleId = request.param("name");
        ExecuteSnapshotLifecycleAction.Request req = new ExecuteSnapshotLifecycleAction.Request(snapLifecycleId);
        req.timeout(request.paramAsTime("timeout", req.timeout()));
        req.masterNodeTimeout(request.paramAsTime("master_timeout", req.masterNodeTimeout()));
        return channel -> client.execute(ExecuteSnapshotLifecycleAction.INSTANCE, req, new RestToXContentListener<>(channel));
    }
}
