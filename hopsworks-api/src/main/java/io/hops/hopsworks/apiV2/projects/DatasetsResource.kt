package io.hops.hopsworks.apiV2.projects


import io.hops.hopsworks.api.filter.AllowedRoles
import io.hops.hopsworks.api.filter.NoCacheResponse
import io.hops.hopsworks.api.project.DataSetService
import io.hops.hopsworks.api.util.JsonResponse
import io.hops.hopsworks.apiV2.jsonOk
import io.hops.hopsworks.common.constants.message.ResponseMessages
import io.hops.hopsworks.common.dao.dataset.DataSetDTO
import io.hops.hopsworks.common.dao.dataset.DataSetView
import io.hops.hopsworks.common.dao.dataset.Dataset
import io.hops.hopsworks.common.dao.dataset.DatasetFacade
import io.hops.hopsworks.common.dao.hdfs.inode.Inode
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade
import io.hops.hopsworks.common.dao.hdfs.inode.InodeView
import io.hops.hopsworks.common.dao.project.Project
import io.hops.hopsworks.common.dao.project.ProjectFacade
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade
import io.hops.hopsworks.common.dao.user.UserFacade
import io.hops.hopsworks.common.dao.user.security.ua.UserManager
import io.hops.hopsworks.common.dataset.DatasetController
import io.hops.hopsworks.common.exception.AppException
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps
import io.hops.hopsworks.common.hdfs.DistributedFsService
import io.hops.hopsworks.common.hdfs.HdfsUsersController
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.apache.hadoop.fs.permission.FsAction
import org.apache.hadoop.fs.permission.FsPermission
import org.apache.hadoop.security.AccessControlException

import javax.ejb.EJB
import javax.ejb.TransactionAttribute
import javax.ejb.TransactionAttributeType
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.GenericEntity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext
import javax.ws.rs.core.UriBuilder

import java.io.IOException
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger

@Api(value = "V2 Datasets", tags = arrayOf("V2 Datasets"))
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
class DatasetsResource{

    @EJB
    private lateinit var projectFacade: ProjectFacade
    @EJB
    private lateinit var hdfsUsersBean: HdfsUsersController
    @EJB
    private lateinit var userBean: UserManager
    @EJB
    private lateinit var noCacheResponse: NoCacheResponse
    @EJB
    private lateinit var datasetController: DatasetController
    @EJB
    private lateinit var datasetFacade: DatasetFacade
    @EJB
    private lateinit var dfs: DistributedFsService
    @EJB
    private lateinit var pathValidator: PathValidatorV2
    @EJB
    private lateinit var projectTeamFacade: ProjectTeamFacade
    @EJB
    private lateinit var inodes: InodeFacade
    @EJB
    private lateinit var userFacade: UserFacade
    @Inject
    private lateinit var dataSetService: DataSetService
    @Inject
    private lateinit var blobsResource: BlobsResource

    var projectId: Int? = null
        set(projectId) {
            field = projectId
            this.project = this.projectFacade.find(projectId)
        }
    private var project: Project? = null


    @ApiOperation("Get a list of data sets in project")
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = arrayOf(AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER))
    fun getDataSets(@Context sc: SecurityContext): Response {
        val dsViews = project!!.datasetCollection.map { DataSetView(it) }
        val result = object : GenericEntity<List<DataSetView>>(dsViews) {}
        return jsonOk(result)
    }

    @ApiOperation("Create a data set")
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = arrayOf(AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER))
    @Throws(AppException::class)
    fun createDataSet(dataSetDTO: DataSetDTO,
                      @Context sc: SecurityContext,
                      @Context req: HttpServletRequest): Response {

        val user = userBean.getUserByEmail(sc.userPrincipal.name)
        val dfso = dfs.dfsOps
        val username = hdfsUsersBean.getHdfsUserName(project, user) ?: throw AppException(Response.Status.BAD_REQUEST.statusCode, "User not found")
        val udfso = dfs.getDfsOps(username)

        try {
            datasetController.createDataset(user, project, dataSetDTO.name,
                    dataSetDTO.description, dataSetDTO.template, dataSetDTO.isSearchable,
                    false, dfso) // both are dfso to create it as root user

            //Generate README.md for the dataset if the user requested it
            if (dataSetDTO.isGenerateReadme) {
                //Persist README.md to hdfs
                datasetController.generateReadme(udfso, dataSetDTO.name, dataSetDTO.description,
                        project!!.name)
            }
        } catch (e: IOException) {
            throw AppException(Response.Status.INTERNAL_SERVER_ERROR.statusCode, "Failed to create dataset: " + e.localizedMessage)
        } finally {
            dfso?.close()
            if (udfso != null) {
                dfs.closeDfsClient(udfso)
            }
        }

        val created = object : GenericEntity<DataSetView>(DataSetView(getDataSet(dataSetDTO
                .name))) {

        }

        val builder = UriBuilder.fromResource(DatasetsResource::class.java)
        val uri = builder.path(DatasetsResource::class.java, "/").build(dataSetDTO.name)

        return Response.created(uri).type(MediaType.APPLICATION_JSON_TYPE).entity(created).build()
    }

    @ApiOperation("Get data set metadata")
    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = arrayOf(AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER))
    @Throws(AppException::class)
    fun getDataSet(@PathParam("name") name: String, @Context
    sc: SecurityContext): Response {

        val toReturn = getDataSet(name)
        val dsView = object : GenericEntity<DataSetView>(DataSetView(toReturn)) {

        }

        return Response.ok(dsView, MediaType.APPLICATION_JSON_TYPE).build()
    }

    @Throws(AppException::class)
    private fun getDataSet(name: String): Dataset {

        return datasetFacade.findByNameAndProjectId(project, name)
                ?: throw AppException(Response.Status.NOT_FOUND, "Data set with name $name can not be found.")
    }

    @ApiOperation("Update data set metadata")
    @POST
    @Path("/{name}")
    @Throws(AppException::class)
    fun updateDataSet(@PathParam("name") name: String, update: DataSetDTO, @Context sc: SecurityContext): Response {
        throw AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.")
    }

    /**
     * This function is used only for deletion of dataset directories
     * as it does not accept a path
     * @param name
     * @param sc
     * @param req
     * @return
     * @throws io.hops.hopsworks.common.exception.AppException
     * @throws org.apache.hadoop.security.AccessControlException
     */
    @ApiOperation("Delete data set")
    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = arrayOf(AllowedRoles.DATA_OWNER))
    @Throws(AppException::class, AccessControlException::class)
    fun deleteDataSet(
            @PathParam("name") name: String,
            @Context
            sc: SecurityContext,
            @Context
            req: HttpServletRequest): Response {

        var success = false
        val json = JsonResponse()
        val dataset = getDataSet(name)
        val path = DataSetPath(dataset, "/")

        val fullPath = pathValidator.getFullPath(path)

        if (dataset.isShared) {
            // The user is trying to delete a dataset. Drop it from the table
            // But leave it in hopsfs because the user doesn't have the right to delete it
            hdfsUsersBean.unShareDataset(project, dataset)
            datasetFacade.removeDataset(dataset)
            json.successMessage = ResponseMessages.SHARED_DATASET_REMOVED
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build()
        }

        var dfso: DistributedFileSystemOps? = null
        try {
            //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
            val user = userBean.getUserByEmail(sc.userPrincipal.name)
            val username = hdfsUsersBean.getHdfsUserName(project, user)
            //If a Data Scientist requested it, do it as project user to avoid deleting Data Owner files
            //Find project of dataset as it might be shared
            val owning = datasetController.getOwningProject(dataset)
            val isMember = projectTeamFacade.isUserMemberOfProject(owning, user)
            if (isMember && projectTeamFacade.findCurrentRole(owning, user) == AllowedRoles.DATA_OWNER
                    && owning == project) {
                dfso = dfs.dfsOps// do it as super user
            } else {
                dfso = dfs.getDfsOps(username)// do it as project user
            }
            success = datasetController.deleteDatasetDir(dataset, fullPath, dfso)
        } catch (ex: AccessControlException) {
            throw AccessControlException(
                    "Permission denied: You can not delete the file " + fullPath.toString())
        } catch (ex: IOException) {
            throw AppException(Response.Status.BAD_REQUEST.statusCode,
                    "Could not delete the file at " + fullPath.toString())
        } finally {
            if (dfso != null) {
                dfs.closeDfsClient(dfso)
            }
        }

        if (!success) {
            throw AppException(Response.Status.BAD_REQUEST.statusCode,
                    "Could not delete the file at " + fullPath.toString())
        }

        //remove the group associated with this dataset as it is a toplevel ds
        try {
            hdfsUsersBean.deleteDatasetGroup(dataset)
        } catch (ex: IOException) {
            //FIXME: take an action?
            logger.log(Level.WARNING,
                    "Error while trying to delete a dataset group", ex)
        }

        json.successMessage = ResponseMessages.DATASET_REMOVED_FROM_HDFS
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build()
    }

    @ApiOperation(value = "Get data set README-file with meta data")
    @GET
    @Path("/{name}/readme")
    @Produces(MediaType.APPLICATION_JSON)
    @Throws(AppException::class)
    fun getReadme(@PathParam("name") datasetName: String, @Context sc: SecurityContext): Response {
        throw AppException(Response.Status.NOT_IMPLEMENTED, "Endpoint not implemented yet.")
    }

    @ApiOperation("Get a list of projects that share this data set")
    @GET
    @Path("/{name}/projects")
    @Produces(MediaType.APPLICATION_JSON)
    @Throws(AppException::class, AccessControlException::class)
    fun getProjects(
            @PathParam("name") name: String,
            @Context sc: SecurityContext,
            @Context req: HttpServletRequest): Response {
        val ds = getDataSet(name)

        val list = datasetFacade.findProjectSharedWith(project, ds.inode)
        val projects = object : GenericEntity<List<Project>>(
                list!!) {

        }

        return Response.ok(projects, MediaType.APPLICATION_JSON_TYPE).build()
    }

    @ApiOperation("Check if data set readonly")
    @GET
    @Path("/{name}/readonly")
    @Throws(AppException::class)
    fun isReadonly(@PathParam("name") name: String, @Context sc: SecurityContext): Response {

        throw AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.")
        //if readonly, return no-content
        //    boolean isReadonly = false; //TODO: add logic..
        //
        //    if (isReadonly){
        //      return Response.noContent().build();
        //    } else {
        //      throw new AppException(Response.Status.NOT_FOUND, "Data set editable");
        //    }
    }

    @ApiOperation("Make data set readonly")
    @PUT
    @Path("/{name}/readonly")
    @Throws(AppException::class, AccessControlException::class)
    fun makeReadonly(@PathParam("name") name: String): Response {
        val dataSet = getDataSet(name)
        datasetController.changeEditable(dataSet, false)

        var dfso: DistributedFileSystemOps? = null
        try {
            // change the permissions as superuser
            dfso = dfs.dfsOps
            val fsPermission = FsPermission(FsAction.ALL,
                    FsAction.READ_EXECUTE,
                    FsAction.NONE, false)
            datasetController.recChangeOwnershipAndPermission(
                    datasetController.getDatasetPath(dataSet),
                    fsPermission, null, null, null, dfso)
            datasetController.changeEditable(dataSet, false)
        } catch (ex: AccessControlException) {
            throw AccessControlException(
                    "Permission denied: Can not change the permission of this file.")
        } catch (e: IOException) {
            throw AppException(Response.Status.INTERNAL_SERVER_ERROR.statusCode, "Error while creating directory: " + e.localizedMessage)
        } finally {
            if (dfso != null) {
                dfso.close()
            }
        }
        return Response.noContent().build()
    }

    @ApiOperation("Make data set editable")
    @DELETE
    @Path("/{name}/readonly")
    @Throws(AppException::class, AccessControlException::class)
    fun makeEditable(@PathParam("name") name: String): Response {
        val dataSet = getDataSet(name)
        var dfso: DistributedFileSystemOps? = null
        try {
            dfso = dfs.dfsOps
            // Change permission as super user
            val fsPermission = FsPermission(FsAction.ALL, FsAction.ALL,
                    FsAction.NONE, true)
            datasetController.recChangeOwnershipAndPermission(
                    datasetController.getDatasetPath(dataSet),
                    fsPermission, null, null, null, dfso)
            datasetController.changeEditable(dataSet, true)
        } catch (ex: AccessControlException) {
            throw AccessControlException(
                    "Permission denied: Can not change the permission of this file.")
        } catch (e: IOException) {
            throw AppException(Response.Status.INTERNAL_SERVER_ERROR.statusCode, "Error while creating directory: " + e.localizedMessage)
        } finally {
            if (dfso != null) {
                dfso.close()
            }
        }

        return Response.noContent().build()
    }


    //File operations
    @ApiOperation("Get data set file/dir listing")
    @GET
    @Path("/{name}/files")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = arrayOf(AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER))
    @Throws(AppException::class)
    fun getDatasetRoot(@PathParam("name") name: String, @Context sc: SecurityContext): Response {
        val dataset = getDataSet(name)
        val path = DataSetPath(dataset, "/")

        val fullPath = pathValidator.getFullPath(path).toString()
        logger.info("XXX: FULL PATH: " + fullPath)

        val inode = pathValidator.exists(path, inodes, true)

        val entity = getDir(inode, fullPath, dataset.isShared)
        return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build()
    }

    @ApiOperation("Get a listing for a path in a data set")
    @GET
    @Path("/{name}/files/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles = arrayOf(AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER))
    @Throws(AppException::class, AccessControlException::class)
    fun getFileOrDir(@PathParam("name") name: String,
                     @PathParam("path") relativePath: String,
                     @Context sc: SecurityContext): Response {

        val dataSet = getDataSet(name)
        val path = DataSetPath(dataSet, relativePath)
        val fullPath = pathValidator.getFullPath(path).toString()
        logger.info("XXX: FULL PATH GET FILE OR DIR: " + fullPath)

        val inode = pathValidator.exists(path, inodes, null)


        if (inode.isDir) {
            val entity = getDir(inode, fullPath, dataSet.isShared)
            return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build()
        } else {
            val entity = getFile(inode, fullPath)
            return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build()
        }
    }

    @ApiOperation("Delete a file or directory")
    @DELETE
    @Path("/{name}/files/{path: .+}")
    @Throws(AccessControlException::class, AppException::class)
    fun deleteFileOrDir(@PathParam("name") dataSetName: String, @PathParam("path") path: String, @Context
    sc: SecurityContext, @Context req: HttpServletRequest): Response {
        return dataSetService.removefile(dataSetName + "/" + path, sc, req)
    }

    @ApiOperation(value = "Copy, Move, Zip or Unzip", notes = "Performs the selected operation on the file/dir " + "specified in the src parameter. All operations are data set scoped. ")
    @PUT
    @Path("/{name}/files/{target: .+}")
    @Throws(AppException::class)
    fun copyMovePutZipUnzip(@PathParam("name") dataSetName: String, @PathParam("target") target: String,
                            @ApiParam(allowableValues = "copy,move,zip,unzip") @QueryParam("op") operation: String?, @QueryParam("src")
                            sourcePath: String): Response {
        if (operation == null) {
            throw AppException(Response.Status.BAD_REQUEST, "?op= parameter required, possible options: " + "copy|move|zip|unzip")
        }
        when (operation) {
            "copy" -> return copy(dataSetName, target, sourcePath)
            "move" -> return move(dataSetName, target, sourcePath)
            "zip" -> return zip(dataSetName, target, sourcePath)
            "unzip" -> return unzip(dataSetName, target, sourcePath)
            else -> throw AppException(Response.Status.BAD_REQUEST, "?op= parameter should be one of: copy|move|zip|unzip")
        }
    }

    @Throws(AppException::class)
    private fun copy(dataSet: String, targetPath: String, sourcePath: String): Response {
        throw AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.")
    }

    @Throws(AppException::class)
    private fun move(dataSet: String, targetPath: String, sourcePath: String): Response {
        throw AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.")
    }

    @Throws(AppException::class)
    private fun zip(dataSet: String, targetPath: String, sourcePath: String): Response {
        throw AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.")
    }

    @Throws(AppException::class)
    private fun unzip(dataSet: String, targetPath: String, sourcePath: String): Response {
        throw AppException(Response.Status.NOT_IMPLEMENTED, "Not implemented yet.")
    }

    @Path("/{name}/blobs")
    @Throws(AppException::class)
    fun blobs(@PathParam("name") dataSetName: String, @Context sc: SecurityContext): BlobsResource {
        val ds = getDataSet(dataSetName)
        this.blobsResource.setProject(project)
        this.blobsResource.setDataset(ds)
        return this.blobsResource
    }

    private fun getFile(inode: Inode, path: String): GenericEntity<InodeView> {
        val inodeView = InodeView(inode, path + "/" + inode.inodePK.name)
        //inodeView.setUnzippingState(settings.getUnzippingState(
        //    path+ "/" + inode.getInodePK().getName()));
        val user = userFacade.findByUsername(inodeView.owner)
        if (user != null) {
            inodeView.owner = user.fname + " " + user.lname
            inodeView.email = user.email
        }

        return object : GenericEntity<InodeView>(inodeView) {

        }
    }

    private fun getDir(inode: Inode,
                       path: String, isShared: Boolean): GenericEntity<List<InodeView>> {
        val cwdChildren = inodes.getChildren(inode)

        val kids = ArrayList<InodeView>()
        for (i in cwdChildren) {
            val inodeView = InodeView(i, path + "/" + i.inodePK
                    .name)
            if (isShared) {
                //Get project of project__user the inode is owned by
                inodeView.owningProjectName = hdfsUsersBean.getProjectName(i.hdfsUser.name)
            }
            //inodeView.setUnzippingState(settings.getUnzippingState(
            //    path + "/" + i.getInodePK().getName()));
            val user = userFacade.findByUsername(inodeView.owner)
            if (user != null) {
                inodeView.owner = user.fname + " " + user.lname
                inodeView.email = user.email
            }
            kids.add(inodeView)
        }
        return object : GenericEntity<List<InodeView>>(kids) {

        }
    }

    companion object {

        private val logger = Logger.getLogger(DatasetsResource::class.java.name)
    }


}
