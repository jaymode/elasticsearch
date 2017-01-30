/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.reindex;

import org.elasticsearch.action.bulk.byscroll.AbstractAsyncBulkByScrollAction;
import org.elasticsearch.action.bulk.byscroll.AbstractAsyncBulkByScrollActionMetadataTestCase;
import org.elasticsearch.action.bulk.byscroll.BulkByScrollResponse;
import org.elasticsearch.action.bulk.byscroll.ScrollableHitSource.Hit;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;

public class UpdateByQueryMetadataTests
        extends AbstractAsyncBulkByScrollActionMetadataTestCase<UpdateByQueryRequest, BulkByScrollResponse> {
    public void testRoutingIsCopied() throws Exception {
        IndexRequest index = new IndexRequest();
        action().copyMetadata(AbstractAsyncBulkByScrollAction.wrap(index), doc().setRouting("foo"));
        assertEquals("foo", index.routing());
    }

    @Override
    protected TestAction action() {
        return new TestAction();
    }

    @Override
    protected UpdateByQueryRequest request() {
        return new UpdateByQueryRequest(new SearchRequest());
    }

    private class TestAction extends TransportUpdateByQueryAction.AsyncIndexBySearchAction {
        public TestAction() {
            super(UpdateByQueryMetadataTests.this.task, UpdateByQueryMetadataTests.this.logger, null,
                    UpdateByQueryMetadataTests.this.threadPool, request(), null, null, listener());
        }

        @Override
        public AbstractAsyncBulkByScrollAction.RequestWrapper<?> copyMetadata(AbstractAsyncBulkByScrollAction.RequestWrapper<?> request,
                Hit doc) {
            return super.copyMetadata(request, doc);
        }
    }
}
