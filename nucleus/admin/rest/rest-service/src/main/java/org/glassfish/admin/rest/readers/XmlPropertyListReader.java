/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.admin.rest.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.glassfish.admin.rest.client.utils.MarshallingUtils;
import org.glassfish.admin.rest.utils.Util;

/**
 *
 * @author jasonlee
 */
@Consumes(MediaType.APPLICATION_XML)
@Provider
public class XmlPropertyListReader implements MessageBodyReader<List<Map<String, String>>> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(List.class) && Map.class.isAssignableFrom(Util.getFirstGenericType(genericType));
    }

    @Override
    public List<Map<String, String>> readFrom(Class<List<Map<String, String>>> type, Type genericType,
        Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> headers,
        InputStream in) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                line = reader.readLine();
            }

            return MarshallingUtils.getPropertiesFromXml(sb.toString());

        } catch (Exception exception) {
            throw new WebApplicationException(exception, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
