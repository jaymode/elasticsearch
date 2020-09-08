/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.compat;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.Version;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.RestCompatibilityPlugin;
import org.elasticsearch.rest.RestStatus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompatibleVersionPlugin extends Plugin implements RestCompatibilityPlugin {

    private static final Pattern COMPATIBLE_API_HEADER_PATTERN = Pattern.compile(
        "(application|text)/(vnd.elasticsearch\\+)?([^;]+)(\\s*;\\s*compatible-with=(\\d+))?",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public Version getCompatibleVersion(@Nullable String acceptHeader, @Nullable String contentTypeHeader, Boolean hasContent)
        throws ElasticsearchStatusException {
        String aVersion = parseVersion(acceptHeader);
        byte acceptVersion = aVersion == null ? Version.CURRENT.major : Integer.valueOf(aVersion).byteValue();
        String cVersion = parseVersion(contentTypeHeader);
        byte contentTypeVersion = cVersion == null ? Version.CURRENT.major : Integer.valueOf(cVersion).byteValue();

        // accept version must be current or prior
        if (acceptVersion > Version.CURRENT.major || acceptVersion < Version.CURRENT.major - 1) {
            throw new ElasticsearchStatusException(
                "Compatible version must be equal or less then the current version. Accept={}} Content-Type={}}",
                RestStatus.BAD_REQUEST,
                acceptHeader,
                contentTypeHeader
            );
        }
        if (hasContent) {

            // content-type version must be current or prior
            if (contentTypeVersion > Version.CURRENT.major || contentTypeVersion < Version.CURRENT.major - 1) {
                throw new ElasticsearchStatusException(
                    "Compatible version must be equal or less then the current version. Accept={} Content-Type={}",
                    RestStatus.BAD_REQUEST,
                    acceptHeader,
                    contentTypeHeader,
                    RestStatus.BAD_REQUEST
                );
            }
            // if both accept and content-type are sent, the version must match
            if (contentTypeVersion != acceptVersion) {
                throw new ElasticsearchStatusException(
                    "Content-Type and Accept version requests have to match. Accept={} Content-Type={}",
                    RestStatus.BAD_REQUEST,
                    acceptHeader,
                    contentTypeHeader
                );
            }
            // both headers should be versioned or none
            if ((cVersion == null && aVersion != null) || (aVersion == null && cVersion != null)) {
                throw new ElasticsearchStatusException(
                    "Versioning is required on both Content-Type and Accept headers. Accept={} Content-Type={}",
                    RestStatus.BAD_REQUEST,
                    acceptHeader,
                    contentTypeHeader
                );
            }
            if (contentTypeVersion < Version.CURRENT.major) {
                return Version.CURRENT.previousMajor();
            }
        }

        if (acceptVersion < Version.CURRENT.major) {
            return Version.CURRENT.previousMajor();
        }

        return Version.CURRENT;
    }

    private static String parseVersion(String mediaType) {
        if (mediaType != null) {
            Matcher matcher = COMPATIBLE_API_HEADER_PATTERN.matcher(mediaType);
            if (matcher.find() && "vnd.elasticsearch+".equalsIgnoreCase(matcher.group(2))) {
                return matcher.group(5);
            }
        }
        return null;
    }
}
