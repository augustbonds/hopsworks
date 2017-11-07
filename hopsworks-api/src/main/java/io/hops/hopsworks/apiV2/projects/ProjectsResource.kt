package io.hops.hopsworks.apiV2.projects

import com.fasterxml.jackson.annotation.JsonProperty
import io.hops.hopsworks.api.filter.AllowedRoles
import io.hops.hopsworks.apiV2.isAdmin
import io.hops.hopsworks.apiV2.jsonOk
import io.hops.hopsworks.apiV2.users.UserController
import io.hops.hopsworks.apiV2.users.UserView
import io.hops.hopsworks.common.dao.dataset.Dataset
import io.hops.hopsworks.common.dao.project.Project
import io.hops.hopsworks.common.dao.project.service.ProjectServices
import io.hops.hopsworks.common.dao.project.team.ProjectTeam
import io.hops.hopsworks.common.dao.user.activity.Activity
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.ejb.EJB
import javax.ejb.Stateless
import javax.ejb.TransactionAttribute
import javax.ejb.TransactionAttributeType
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.*

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Path("/v2/projects")
@RolesAllowed("HOPS_ADMIN", "HOPS_USER")
@Api(value = "V2 Projects")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ProjectsResource {
    @EJB
    private lateinit var controller: ProjectController
    @EJB
    private lateinit var userController: UserController

    @GET
    @ApiOperation(value = "Get a list of projects")
    @AllowedRoles(roles = arrayOf(AllowedRoles.ALL))
    fun getProjects(@Context sc: SecurityContext): Response {

        val projects = controller.getAll()
        val result = if (isAdmin(sc)) {
            val projectViews = projects.map { ProjectView(it) }
            object : GenericEntity<List<ProjectView>>(projectViews) {}
        } else {
            val projectViews = projects.map { LimitedProjectView(it) }
            object : GenericEntity<List<LimitedProjectView>>(projectViews) {}
        }

        return jsonOk(result)
    }


    @POST
    @ApiOperation(value = "Create a project")
    @AllowedRoles(roles = arrayOf(AllowedRoles.ALL))
    fun createProject(projectView: ProjectView?, @Context sc: SecurityContext?, @Context req: HttpServletRequest?,
                      @QueryParam("template") starterType: String?): Response {
        if (sc == null || sc.userPrincipal){

        }
        starterType?.let{
            if (!it.isEmpty()){
                return createStarterProject(sc, it)
            }
        }

        val user = userController.findByEmail(sc.userPrincipal.name)



    }

    private fun createStarterProject(sc: SecurityContext, starterType: String) : Response {

    }
}

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

class LimitedProjectView(
        @param:JsonProperty("projectId") val projectId: Int,
        @param:JsonProperty("description") val description: String,
        @param:JsonProperty("name") val name: String,
        @param:JsonProperty("owner") val owner: UserView
)

fun LimitedProjectView(project: Project): LimitedProjectView {
    val owner = UserView(project.owner)
    return LimitedProjectView(project.id, project.description, project.name, owner)
}
