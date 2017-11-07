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
package io.hops.hopsworks.apiV2.projects

import com.fasterxml.jackson.annotation.JsonProperty
import io.hops.hopsworks.apiV2.users.UserView
import io.hops.hopsworks.common.dao.dataset.Dataset
import io.hops.hopsworks.common.dao.project.Project
import io.hops.hopsworks.common.dao.project.service.ProjectServices
import io.hops.hopsworks.common.dao.project.team.ProjectTeam
import io.hops.hopsworks.common.dao.user.activity.Activity


import java.util.Date


class ProjectView(
        @param:JsonProperty("projectId") var projectId: Int,
        @param:JsonProperty("description") var description: String,
        @param:JsonProperty("created") var created: Date,
        @param:JsonProperty("ethicalStatus") var ethicalStatus: String,
        @param:JsonProperty("isArchived") var isArchived: Boolean,
        @param:JsonProperty("name") val name: String,
        @param:JsonProperty("owner") val owner: UserView,
        @param:JsonProperty("team") val team: List<ProjectTeam>,
        @param:JsonProperty("services") val services: List<ProjectServices>,
        @param:JsonProperty("activity") val activity: List<Activity>,
        @param:JsonProperty("datasets") val datasets: List<Dataset>
)

fun ProjectView(project: Project): ProjectView {
    return ProjectView(
            project.id,
            project.description,
            project.created,
            project.ethicalStatus,
            project.archived,
            project.name,
            UserView(project.owner),
            project.projectTeamCollection.map { it },
            project.projectServicesCollection.map { it },
            project.activityCollection.map { it },
            project.datasetCollection.map { it }
    )

}