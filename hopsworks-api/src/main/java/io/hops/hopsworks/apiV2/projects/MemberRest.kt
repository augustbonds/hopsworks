/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.apiV2.projects

import com.fasterxml.jackson.annotation.JsonProperty
import io.hops.hopsworks.apiV2.users.UserRest
import io.hops.hopsworks.apiV2.users.fromUsers
import io.hops.hopsworks.common.dao.project.team.ProjectTeam

class MemberRest (
    @param:JsonProperty("user") val user: UserRest,
    @param:JsonProperty("role") val role: String
)

fun fromProjectTeam(projectTeam: ProjectTeam): MemberRest {
    val user = projectTeam.user
    return MemberRest(fromUsers(user), projectTeam.teamRole)
}
