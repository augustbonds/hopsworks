/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.apiV2

import io.hops.hopsworks.common.exception.AppException

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.net.URI
import javax.ws.rs.core.SecurityContext

fun except(status: Response.Status, msg: String) {
    throw AppException(status.statusCode, msg)
}

fun jsonOk(entity: Any): Response {
    return Response.ok(entity).type(MediaType.APPLICATION_JSON_TYPE).build()
}

fun jsonCreated(location: URI, entity: Any): Response {
    return Response.created(location).entity(entity).type(MediaType.APPLICATION_JSON_TYPE).build()
}

fun isAdmin(sc: SecurityContext): Boolean {
    return sc.isUserInRole("HOPS_ADMIN")
}
