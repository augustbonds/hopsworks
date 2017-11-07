package io.hops.hopsworks.common.dao.dataset;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.project.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class DatasetFacade extends AbstractFacade<Dataset> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public DatasetFacade() {
    super(Dataset.class);
  }

  @Override
  public List<Dataset> findAll() {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAll",
      Dataset.class);
    return query.getResultList();
  }

  /**
   * Finds a dataset by id
   * <p/>
   * @param id
   * @return
   */
  public Dataset find(Integer id) {
    return em.find(Dataset.class, id);
  }

  /**
   * Finds all instances of a dataset. i.e if a dataset is shared it is going
   * to be present in the parent project and in the project it is shared with.
   * <p/>
   * @param inode
   * @return
   */
  public Dataset findByInode(Inode inode) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByInode",
      Dataset.class).setParameter(
        "inode", inode);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public Dataset findByInodeId(int inodeId) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByInodeId",
      Dataset.class).setParameter(
        "inodeId", inodeId);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public Optional<Dataset> findByPublicDsIdProject(String publicDsId, Project project) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByPublicDsIdProject", Dataset.class)
      .setParameter("publicDsId", publicDsId)
      .setParameter("project", project);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<Dataset> findByPublicDsId(String publicDsId) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByPublicDsId", Dataset.class)
      .setParameter("publicDsId", publicDsId);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Dataset findByNameAndProjectIdGottaFix(Project project, String name) {
    TypedQuery<Dataset> query = em.createNamedQuery(
      "Dataset.findByNameAndProjectId",
      Dataset.class);
    query.setParameter("name", name).setParameter("projectId", project);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<Project> findProjectSharedWith(Project project, Inode inode) {
    Dataset datasets = findByInode(inode);
    List<Project> sharedWith = new ArrayList<>();
    for (DatasetProjectAssociation datasetProjectAssociation : datasets.getSharedWith()) {
      sharedWith.add(datasetProjectAssociation.getProject());
    }
    return sharedWith;
  }

  /**
   * Find by project and dataset name
   * <p/>
   * @param project
   * @param inode
   * @return
   */
  public Dataset findByProjectAndInode(Project project, Inode inode) {
    try {
      return em.createNamedQuery("Dataset.findByProjectAndInode", Dataset.class)
        .setParameter("projectId", project).setParameter(
          "inode", inode).getSingleResult();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Finds all data sets in a project.
   * <p/>
   * @param project
   * @return
   */
  public List<Dataset> findByProject(Project project) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findByProject",
      Dataset.class).setParameter(
        "projectId", project);
    return query.getResultList();
  }

  public List<DataSetDTO> findPublicDatasets() {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAllPublic",
      Dataset.class);
    List<Dataset> datasets = query.getResultList();

    List<DataSetDTO> ds = new ArrayList<>();
    for (Dataset d : datasets) {
      DataSetDTO dto = new DataSetDTO();
      dto.setDescription(d.getDescription());
      dto.setName(d.getInode().getInodePK().getName());
      dto.setInodeId(d.getInode().getId());
      ds.add(dto);
    }
    return ds;
  }
  
  public List<Dataset> findAllPublicDatasets() {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAllPublic", Dataset.class);
    return query.getResultList();   
  }
  
  public List<Dataset> findAllDatasetsByState(int state, boolean shared) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAllByState", Dataset.class)
      .setParameter("state", state)
      .setParameter("shared", shared);
    return query.getResultList();   
  }

  /**
   * Finds all data sets shared with a project.
   * <p/>
   * @param project
   * @return
   */
  public List<Dataset> findSharedWithProject(Project project) {
    TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findSharedWithProject", Dataset.class).setParameter(
      "projectId", project);
    return query.getResultList();
  }

  public void persistDataset(Dataset dataset) {
    em.persist(dataset);
  }

  public void flushEm() {
    em.flush();
  }

  public void merge(Dataset dataset) {
    em.merge(dataset);
    em.flush();
  }

  public void removeDataset(Dataset dataset) {
    Dataset ds = em.find(Dataset.class, dataset.getId());
    if (ds != null) {
      em.remove(ds);
    }
  }
}
