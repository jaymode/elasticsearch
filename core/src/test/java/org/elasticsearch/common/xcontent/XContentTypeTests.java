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
package org.elasticsearch.common.xcontent;

import org.elasticsearch.test.ESTestCase;

import java.util.Locale;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class XContentTypeTests extends ESTestCase {
    public void testFromJson() throws Exception {
        String mediaType = "application/json";
        XContentType expectedXContentType = XContentType.JSON;
        assertThat(XContentType.fromContentType(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromContentType(mediaType + ";"), equalTo(expectedXContentType));
        assertThat(XContentType.fromContentType(mediaType + "; charset=UTF-8"), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + ";"), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + "; charset=UTF-8"), equalTo(expectedXContentType));
        assertNull(XContentType.fromFormat(mediaType));
        assertThat(XContentType.fromFormat("json"), equalTo(expectedXContentType));
    }

    public void testFromJsonUppercase() throws Exception {
        String mediaType = "application/json".toUpperCase(Locale.ROOT);
        XContentType expectedXContentType = XContentType.JSON;
        assertThat(XContentType.fromContentType(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromContentType(mediaType + ";"), equalTo(expectedXContentType));
        assertThat(XContentType.fromContentType(mediaType + "; charset=UTF-8"), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + ";"), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + "; charset=UTF-8"), equalTo(expectedXContentType));
        assertNull(XContentType.fromFormat(mediaType));
        assertThat(XContentType.fromFormat("json".toUpperCase(Locale.ROOT)), equalTo(expectedXContentType));
    }

    public void testFromYaml() throws Exception {
        String mediaType = "application/yaml";
        XContentType expectedXContentType = XContentType.YAML;
        assertThat(XContentType.fromContentType(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromContentType(mediaType + ";"), equalTo(expectedXContentType));
        assertThat(XContentType.fromContentType(mediaType + "; charset=UTF-8"), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + ";"), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + "; charset=UTF-8"), equalTo(expectedXContentType));
        assertNull(XContentType.fromFormat(mediaType));
        assertThat(XContentType.fromFormat("yaml"), equalTo(expectedXContentType));
    }

    public void testFromSmile() throws Exception {
        String mediaType = "application/smile";
        XContentType expectedXContentType = XContentType.SMILE;
        assertThat(XContentType.fromContentType(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromContentType(mediaType + ";"), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + ";"), equalTo(expectedXContentType));
        assertNull(XContentType.fromFormat(mediaType));
        assertThat(XContentType.fromFormat("smile"), equalTo(expectedXContentType));
    }

    public void testFromCbor() throws Exception {
        String mediaType = "application/cbor";
        XContentType expectedXContentType = XContentType.CBOR;
        assertThat(XContentType.fromContentType(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromContentType(mediaType + ";"), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + ";"), equalTo(expectedXContentType));
        assertNull(XContentType.fromFormat(mediaType));
        assertThat(XContentType.fromFormat("cbor"), equalTo(expectedXContentType));
    }

    public void testFromWildcard() throws Exception {
        String mediaType = "*/*";
        assertNull(XContentType.fromContentType(mediaType));
        assertNull(XContentType.fromContentType(mediaType + ";"));
        XContentType expectedXContentType = XContentType.JSON;
        assertThat(XContentType.fromAccept(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + ";"), equalTo(expectedXContentType));
        assertNull(XContentType.fromFormat(mediaType));
        assertNull(XContentType.fromFormat(mediaType));
    }

    public void testFromApplicationWildcard() throws Exception {
        String mediaType = "application/*";
        assertNull(XContentType.fromContentType(mediaType));
        assertNull(XContentType.fromContentType(mediaType + ";"));
        XContentType expectedXContentType = XContentType.JSON;
        assertThat(XContentType.fromAccept(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + ";"), equalTo(expectedXContentType));
        assertNull(XContentType.fromFormat(mediaType));
    }

    public void testFromApplicationWildcardUppercase() throws Exception {
        String mediaType = "APPLICATION/*";
        assertNull(XContentType.fromContentType(mediaType));
        assertNull(XContentType.fromContentType(mediaType + ";"));
        XContentType expectedXContentType = XContentType.JSON;
        assertThat(XContentType.fromAccept(mediaType), equalTo(expectedXContentType));
        assertThat(XContentType.fromAccept(mediaType + ";"), equalTo(expectedXContentType));
        assertNull(XContentType.fromFormat(mediaType));
    }

    public void testFromRubbish() throws Exception {
        assertThat(XContentType.fromContentType(null), nullValue());
        assertThat(XContentType.fromContentType(""), nullValue());
        assertThat(XContentType.fromContentType("text/plain"), nullValue());
        assertThat(XContentType.fromContentType("gobbly;goop"), nullValue());
        assertThat(XContentType.fromAccept(null), nullValue());
        assertThat(XContentType.fromAccept(""), nullValue());
        assertThat(XContentType.fromAccept("text/plain"), nullValue());
        assertThat(XContentType.fromAccept("gobbly;goop"), nullValue());
        assertThat(XContentType.fromFormat(null), nullValue());
        assertThat(XContentType.fromFormat(""), nullValue());
        assertThat(XContentType.fromFormat("text/plain"), nullValue());
        assertThat(XContentType.fromFormat("gobbly;goop"), nullValue());
    }
}
