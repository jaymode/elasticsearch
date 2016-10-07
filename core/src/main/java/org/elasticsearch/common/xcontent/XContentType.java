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

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.cbor.CborXContent;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.smile.SmileXContent;
import org.elasticsearch.common.xcontent.yaml.YamlXContent;

import java.io.IOException;
import java.util.Locale;

/**
 * The content type of {@link org.elasticsearch.common.xcontent.XContent}.
 */
public enum XContentType implements Writeable {

    /**
     * A JSON based content type.
     */
    JSON(0) {
        @Override
        protected String mediaTypeWithoutParameters() {
            return "application/json";
        }

        @Override
        public String mediaType() {
            return "application/json; charset=UTF-8";
        }

        @Override
        public String shortName() {
            return "json";
        }

        @Override
        public XContent xContent() {
            return JsonXContent.jsonXContent;
        }
    },
    /**
     * The jackson based smile binary format. Fast and compact binary format.
     */
    SMILE(1) {
        @Override
        protected String mediaTypeWithoutParameters() {
            return "application/smile";
        }

        @Override
        public String shortName() {
            return "smile";
        }

        @Override
        public XContent xContent() {
            return SmileXContent.smileXContent;
        }
    },
    /**
     * A YAML based content type.
     */
    YAML(2) {
        @Override
        protected String mediaTypeWithoutParameters() {
            return "application/yaml";
        }

        @Override
        public String shortName() {
            return "yaml";
        }

        @Override
        public XContent xContent() {
            return YamlXContent.yamlXContent;
        }
    },
    /**
     * A CBOR based content type.
     */
    CBOR(3) {
        @Override
        protected String mediaTypeWithoutParameters() {
            return "application/cbor";
        }

        @Override
        public String shortName() {
            return "cbor";
        }

        @Override
        public XContent xContent() {
            return CborXContent.cborXContent;
        }
    };

    /**
     * Attempts to match the string representation of a content type to the appropriate {@link XContentType}. No wildcards will be matched
     * by this method
     */
    public static XContentType fromContentType(String contentType) {
        if (Strings.hasLength(contentType)) {
            final String contentTypeLowerCase = contentType.toLowerCase(Locale.ROOT);
            for (XContentType type : values()) {
                if (isSameMediaTypeAs(contentTypeLowerCase, type)) {
                    return type;
                }
            }
        }

        return null;
    }

    /**
     * Attempts to match the string representation of a content type to the appropriate {@link XContentType}. A match all wildcard
     * or an application type wildcard will be mapped to JSON
     */
    public static XContentType fromAccept(String accept) {
        if (Strings.hasLength(accept)) {
            final String acceptLowerCase = accept.toLowerCase(Locale.ROOT);
            for (XContentType type : values()) {
                if (isSameMediaTypeAs(acceptLowerCase, type)) {
                    return type;
                }
            }

            if (acceptLowerCase.startsWith("*/*") || acceptLowerCase.startsWith("application/*")) {
                return JSON;
            }
        }
        return null;
    }

    /**
     * Attempts to match the short format to the appropriate {@link XContentType}
     */
    public static XContentType fromFormat(String format) {
        if (Strings.hasLength(format)) {
            final String formatLowerCase = format.toLowerCase(Locale.ROOT);
            for (XContentType type : values()) {
                if (formatLowerCase.equals(type.shortName())) {
                    return type;
                }
            }
        }

        return null;
    }

    private static boolean isSameMediaTypeAs(String stringType, XContentType type) {
        return type.mediaTypeWithoutParameters().equals(stringType) ||
                stringType.startsWith(type.mediaTypeWithoutParameters() + ";");
    }

    private int index;

    XContentType(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public String mediaType() {
        return mediaTypeWithoutParameters();
    }

    public abstract String shortName();

    public abstract XContent xContent();

    protected abstract String mediaTypeWithoutParameters();

    public static XContentType readFrom(StreamInput in) throws IOException {
        int index = in.readVInt();
        for (XContentType contentType : values()) {
            if (index == contentType.index) {
                return contentType;
            }
        }
        throw new IllegalStateException("Unknown XContentType with index [" + index + "]");
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(index);
    }
}
