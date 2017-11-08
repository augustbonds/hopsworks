package io.hops.hopsworks.apiV2.projects

import io.hops.hopsworks.common.dao.project.Project
import io.hops.hopsworks.common.dao.project.ProjectFacade
import io.hops.hopsworks.common.project.ProjectDTO
import javax.ejb.EJB
import javax.ejb.Stateless

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
@Stateless
class ProjectController{
    @EJB
    private lateinit var projectFacade: ProjectFacade

    fun getAll(): List<Project> {
        return projectFacade.findAll()?: listOf()
    }

    fun createProject() {

    }

    private fun toProjectDTO(projectView: ProjectView): ProjectDTO{
        projectView.team
        ProjectDTO(projectView.projectId, projectView.name, projectView.owner)
    }
}
