/**
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
package io.hops.hopsworks.common.dao.dataset;

import io.hops.hopsworks.common.dao.project.Project;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name="dataset_shares")
@IdClass(DatasetProjectAssociationId.class)
public class DatasetProjectAssociation {
  public enum Status{
    ACCEPTED,
    PENDING
  }
  @Id
  private long datasetId;
  @Id
  private long projectId;
  
  @Column(name="share_status")
  @Enumerated(EnumType.ORDINAL)
  private Status status;
  
  @ManyToOne
  @PrimaryKeyJoinColumn(name="dataset_id", referencedColumnName = "id")
  private Dataset dataset;
  
  @ManyToOne
  @PrimaryKeyJoinColumn(name="project_id", referencedColumnName = "id")
  private Project project;
  
  public DatasetProjectAssociation(){}
  
  public long getDatasetId() {
    return datasetId;
  }
  
  public void setDatasetId(long datasetId) {
    this.datasetId = datasetId;
  }
  
  public long getProjectId() {
    return projectId;
  }
  
  public void setProjectId(long projectId) {
    this.projectId = projectId;
  }
  
  public Status getStatus() {
    return status;
  }
  
  public void setStatus(Status status) {
    this.status = status;
  }
  
  public Dataset getDataset() {
    return dataset;
  }
  
  public void setDataset(Dataset dataset) {
    this.dataset = dataset;
  }
  
  public Project getProject() {
    return project;
  }
  
  public void setProject(Project project) {
    this.project = project;
  }
}
