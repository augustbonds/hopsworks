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
package io.hops.hopsworks.apiV2.users

import io.hops.hopsworks.api.filter.AllowedRoles
import io.hops.hopsworks.common.dao.user.UserFacade
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.annotation.security.RolesAllowed
import javax.ejb.EJB
import javax.ejb.Stateless
import javax.ejb.TransactionAttribute
import javax.ejb.TransactionAttributeType
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.*

@Path("/v2/users")
@RolesAllowed("HOPS_ADMIN", "HOPS_USER")
@Api(value = "V2 Users", description = "Users Resource")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
open class UsersResource {
    @EJB
    protected lateinit var userBean: UserFacade

    @ApiOperation("Get a list of users in the cluster")
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = arrayOf(AllowedRoles.ALL))
    fun findAll(@Context sc: SecurityContext): Response {

        val users = userBean.findAllUsers()
        val userViews = users.map { user -> UserRest(user.fname,user.lname,user.uid) }

        val result = object : GenericEntity<List<UserRest>>(userViews) {}
        return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build()
    }
}
