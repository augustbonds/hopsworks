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

import com.fasterxml.jackson.annotation.JsonProperty
import io.hops.hopsworks.api.filter.AllowedRoles
import io.hops.hopsworks.apiV2.jsonOk
import io.hops.hopsworks.common.dao.user.Users
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
class UsersResource {
    @EJB
    private lateinit var userController: UserController

    @ApiOperation("Get a list of users in the cluster")
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = arrayOf(AllowedRoles.ALL))
    fun findAll(@Context sc: SecurityContext): Response {

        val users = userController.findAll()
        val userViews = users.map { user -> UserView(user.fname,user.lname,user.uid) }

        val result = object : GenericEntity<List<UserView>>(userViews) {}
        return jsonOk(result)
    }
}

class UserView(
        @param:JsonProperty("firstname") val firstname: String,
        @param:JsonProperty("lastname") val lastname: String,
        @param:JsonProperty("uid") val uid: Int
)

fun UserView(user: Users): UserView {
    return UserView(user.fname, user.lname, user.uid)
}
