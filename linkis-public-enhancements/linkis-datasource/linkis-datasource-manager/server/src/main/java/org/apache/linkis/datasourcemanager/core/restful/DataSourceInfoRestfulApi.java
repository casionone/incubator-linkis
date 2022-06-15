/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.datasourcemanager.core.restful;

import com.github.pagehelper.PageInfo;
import org.apache.linkis.common.exception.ErrorException;
import org.apache.linkis.datasourcemanager.common.ServiceErrorCode;
import org.apache.linkis.datasourcemanager.common.auth.AuthContext;
import org.apache.linkis.datasourcemanager.common.domain.DataSource;
import org.apache.linkis.datasourcemanager.common.domain.DataSourceParamKeyDefinition;
import org.apache.linkis.datasourcemanager.common.domain.DatasourceVersion;
import org.apache.linkis.datasourcemanager.core.formdata.FormDataTransformerFactory;
import org.apache.linkis.datasourcemanager.core.formdata.MultiPartFormDataTransformer;
import org.apache.linkis.datasourcemanager.core.service.DataSourceInfoService;
import org.apache.linkis.datasourcemanager.core.service.DataSourceRelateService;
import org.apache.linkis.datasourcemanager.core.service.MetadataOperateService;
import org.apache.linkis.datasourcemanager.core.service.hooks.DataSourceParamsHook;
import org.apache.linkis.datasourcemanager.core.validate.ParameterValidateException;
import org.apache.linkis.datasourcemanager.core.validate.ParameterValidator;
import org.apache.linkis.datasourcemanager.core.vo.DataSourceVo;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.groups.Default;
import java.io.UnsupportedEncodingException;
import java.util.*;

@RestController
@RequestMapping(value = "/data-source-manager/info")
public class DataSourceInfoRestfulApi {

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceInfoRestfulApi.class);
    @Autowired private DataSourceInfoService dataSourceInfoService;

    @Autowired private DataSourceRelateService dataSourceRelateService;

    @Autowired private ParameterValidator parameterValidator;

    @Autowired private Validator beanValidator;

    @Autowired private MetadataOperateService metadataOperateService;

    private MultiPartFormDataTransformer formDataTransformer;

    @Autowired private List<DataSourceParamsHook> dataSourceParamsHooks = new ArrayList<>();

    @PostConstruct
    public void initRestful() {
        this.formDataTransformer = FormDataTransformerFactory.buildCustom();
    }


    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Message queryDataSource(
            @RequestParam(value = "system", required = false) String createSystem,
            @RequestParam(value = "name", required = false) String dataSourceName,
            @RequestParam(value = "typeId", required = false) Long dataSourceTypeId,
            @RequestParam(value = "identifies", required = false) String identifies,
            @RequestParam(value = "currentPage", required = false) Integer currentPage,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            HttpServletRequest req) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    DataSourceVo dataSourceVo =
                            new DataSourceVo(
                                    dataSourceName, dataSourceTypeId, identifies, createSystem);
                    dataSourceVo.setCurrentPage(null != currentPage ? currentPage : 1);
                    dataSourceVo.setPageSize(null != pageSize ? pageSize : 10);
                    String permissionUser = SecurityFilter.getLoginUsername(req);
                    if (AuthContext.isAdministrator(permissionUser)) {
                        permissionUser = null;
                    }
                    dataSourceVo.setPermissionUser(permissionUser);
                    PageInfo<DataSource> pageInfo =
                            dataSourceInfoService.queryDataSourceInfoPage(dataSourceVo);
                    List<DataSource> queryList = pageInfo.getList();
                    return Message.ok()
                            .data("queryList", queryList)
                            .data("totalPage", pageInfo.getTotal());
                },
                "Fail to query page of data source[查询数据源失败]");
    }



    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Message add(@RequestBody DataSource dataSource, HttpServletRequest req) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    String userName = SecurityFilter.getLoginUsername(req);
                    // Bean validation
                    Set<ConstraintViolation<DataSource>> result =
                            beanValidator.validate(dataSource, Default.class);
                    if (result.size() > 0) {
                        throw new ConstraintViolationException(result);
                    }
                    // Escape the data source name
                    dataSource.setCreateUser(userName);
                    if (dataSourceInfoService.existDataSource(dataSource.getDataSourceName())) {
                        return Message.error(
                                "The data source named: "
                                        + dataSource.getDataSourceName()
                                        + " has been existed [数据源: "
                                        + dataSource.getDataSourceName()
                                        + " 已经存在]");
                    }
                    insertDataSource(dataSource);
                    return Message.ok().data("insertId", dataSource.getId());
                },
                "Fail to insert data source[新增数据源失败]");
    }

    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    public Message updateDataSourceInJson(
            @RequestBody DataSource dataSource,
            @RequestParam("dataSourceId") Long dataSourceId,
            HttpServletRequest req) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    String userName = SecurityFilter.getLoginUsername(req);
                    // Bean validation
                    Set<ConstraintViolation<DataSource>> result =
                            beanValidator.validate(dataSource, Default.class);
                    if (result.size() > 0) {
                        throw new ConstraintViolationException(result);
                    }
                    dataSource.setId(dataSourceId);
                    dataSource.setModifyUser(userName);
                    dataSource.setModifyTime(Calendar.getInstance().getTime());
                    DataSource storedDataSource =
                            dataSourceInfoService.getDataSourceInfoBrief(dataSourceId);
                    if (null == storedDataSource) {
                        return Message.error("This data source was not found [更新数据源失败]");
                    }
                    if (!AuthContext.hasPermission(storedDataSource, userName)) {
                        return Message.error(
                                "Don't have update permission for data source [没有数据源的更新权限]");
                    }
                    String dataSourceName = dataSource.getDataSourceName();
                    if (!Objects.equals(dataSourceName, storedDataSource.getDataSourceName())
                            && dataSourceInfoService.existDataSource(dataSourceName)) {
                        return Message.error(
                                "The data source named: "
                                        + dataSourceName
                                        + " has been existed [数据源: "
                                        + dataSourceName
                                        + " 已经存在]");
                    }
                    dataSourceInfoService.updateDataSourceInfo(dataSource);
                    return Message.ok().data("updateId", dataSourceId);
                },
                "Fail to update data source[更新数据源失败]");
    }


    /**
     * Dangerous operation!
     *
     * @param dataSourceId
     * @return
     */
    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public Message removeDataSource(
            @RequestParam("dataSourceId") Long dataSourceId, HttpServletRequest request) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    // Get brief info
                    DataSource dataSource =
                            dataSourceInfoService.getDataSourceInfoBrief(dataSourceId);

                    if (dataSource == null) {
                        return Message.error("No Exists The DataSource [不存在该数据源]");
                    }

                    if (!AuthContext.hasPermission(dataSource, request)) {
                        return Message.error(
                                "Don't have delete permission for data source [没有数据源的删除权限]");
                    }
                    Long removeId = dataSourceInfoService.removeDataSourceInfo(dataSourceId, "");
                    if (removeId < 0) {
                        return Message.error(
                                "Fail to remove data source[删除数据源信息失败], [id:" + dataSourceId + "]");
                    }
                    return Message.ok().data("removeId", removeId);
                },
                "Fail to remove data source[删除数据源信息失败]");
    }

    @RequestMapping(value = "/expire", method = RequestMethod.PUT)
    public Message expireDataSource(
            @RequestParam("dataSourceId") Long dataSourceId, HttpServletRequest request) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    // Get brief info
                    DataSource dataSource =
                            dataSourceInfoService.getDataSourceInfoBrief(dataSourceId);

                    if (dataSource == null) {
                        return Message.error("No Exists The DataSource [不存在该数据源]");
                    }

                    if (!AuthContext.hasPermission(dataSource, request)) {
                        return Message.error(
                                "Don't have operation permission for data source [没有数据源的操作权限]");
                    }
                    Long expireId = dataSourceInfoService.expireDataSource(dataSourceId);
                    if (expireId < 0) {
                        return Message.error(
                                "Fail to expire data source[数据源过期失败], [id:" + dataSourceId + "]");
                    }
                    return Message.ok().data("expireId", expireId);
                },
                "Fail to expire data source[数据源过期失败]");
    }


    /**
     * create or update parameter, save a version of parameter,return version id.
     *
     * @param params
     * @param req
     * @return
     */
    @RequestMapping(value = "/addParameter", method = RequestMethod.POST)
    public Message addParameter(
            @RequestParam("dataSourceId") Long dataSourceId,
            @RequestBody Map<String, Object> params,
            HttpServletRequest req) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    Map<String, Object> connectParams = (Map) params.get("connectParams");
                    String comment = params.get("comment").toString();
                    String userName = SecurityFilter.getLoginUsername(req);

                    DataSource dataSource =
                            dataSourceInfoService.getDataSourceInfoBrief(dataSourceId);
                    if (null == dataSource) {
                        throw new ErrorException(
                                ServiceErrorCode.DATASOURCE_NOTFOUND_ERROR.getValue(),
                                "datasource not found ");
                    }
                    if (!AuthContext.hasPermission(dataSource, userName)) {
                        return Message.error(
                                "Don't have update permission for data source [没有数据源的更新权限]");
                    }
                    List<DataSourceParamKeyDefinition> keyDefinitionList =
                            dataSourceRelateService.getKeyDefinitionsByType(
                                    dataSource.getDataSourceTypeId());
                    parameterValidator.validate(keyDefinitionList, connectParams);
                    // Encrypt password value type
                    RestfulApiHelper.encryptPasswordKey(keyDefinitionList, connectParams);

                    long versionId =
                            dataSourceInfoService.insertDataSourceParameter(
                                    keyDefinitionList,
                                    dataSourceId,
                                    connectParams,
                                    userName,
                                    comment);

                    return Message.ok().data("version", versionId);
                },
                "Fail to insert data source parameter [保存数据源参数失败]");
    }

    /**
     * get datasource detail, for current version
     *
     * @param dataSourceId
     * @param request
     * @return
     */
    @RequestMapping(value = "/getById", method = RequestMethod.GET)
    public Message getInfoById(
            @RequestParam("dataSourceId") Long dataSourceId, HttpServletRequest request) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    DataSource dataSource = dataSourceInfoService.getDataSourceInfo(dataSourceId);
                    if (dataSource == null) {
                        return Message.error("No Exists The DataSource [不存在该数据源]");
                    }
                    if (!AuthContext.hasPermission(dataSource, request)) {
                        return Message.error(
                                "Don't have query permission for data source [没有数据源的查询权限]");
                    }
                    // Decrypt
                    RestfulApiHelper.decryptPasswordKey(
                            dataSourceRelateService.getKeyDefinitionsByType(
                                    dataSource.getDataSourceTypeId()),
                            dataSource.getConnectParams());
                    return Message.ok().data("info", dataSource);
                },
                "Fail to access data source[获取数据源信息失败]");
    }

    @RequestMapping(value = "/getByName", method = RequestMethod.GET)
    public Message getInfoByDataSourceName(
            @RequestParam("dataSourceName") String dataSourceName, HttpServletRequest request)
            throws UnsupportedEncodingException {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    DataSource dataSource = dataSourceInfoService.getDataSourceInfo(dataSourceName);

                    if (dataSource == null) {
                        return Message.error("No Exists The DataSource [不存在该数据源]");
                    }

                    if (!AuthContext.hasPermission(dataSource, request)) {
                        return Message.error(
                                "Don't have query permission for data source [没有数据源的查询权限]");
                    }
                    // Decrypt
                    RestfulApiHelper.decryptPasswordKey(
                            dataSourceRelateService.getKeyDefinitionsByType(
                                    dataSource.getDataSourceTypeId()),
                            dataSource.getConnectParams());

                    return Message.ok().data("info", dataSource);
                },
                "Fail to access data source[获取数据源信息失败]");
    }

    /**
     * get datasource detail
     *
     * @param dataSourceId
     * @param version
     * @return
     */
    @RequestMapping(value = "/getByVersion", method = RequestMethod.GET)
    public Message getInfoByDataSourceIdAndVersion(
            @RequestParam("dataSourceId") Long dataSourceId,
            @RequestParam("version") Long version,
            HttpServletRequest request) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    DataSource dataSource =
                            dataSourceInfoService.getDataSourceInfo(dataSourceId, version);

                    if (dataSource == null) {
                        return Message.error("No Exists The DataSource [不存在该数据源]");
                    }

                    if (!AuthContext.hasPermission(dataSource, request)) {
                        return Message.error(
                                "Don't have query permission for data source [没有数据源的查询权限]");
                    }
                    // Decrypt
                    RestfulApiHelper.decryptPasswordKey(
                            dataSourceRelateService.getKeyDefinitionsByType(
                                    dataSource.getDataSourceTypeId()),
                            dataSource.getConnectParams());
                    return Message.ok().data("info", dataSource);
                },
                "Fail to access data source[获取数据源信息失败]");
    }

    /**
     * get verion list for datasource
     *
     * @param dataSourceId
     * @param request
     * @return
     */
    @RequestMapping(value = "/getVersions", method = RequestMethod.GET)
    public Message getVersionList(
            @RequestParam("dataSourceId") Long dataSourceId, HttpServletRequest request) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    DataSource dataSource =
                            dataSourceInfoService.getDataSourceInfoBrief(dataSourceId);

                    if (dataSource == null) {
                        return Message.error("No Exists The DataSource [不存在该数据源]");
                    }

                    if (!AuthContext.hasPermission(dataSource, request)) {
                        return Message.error(
                                "Don't have query permission for data source [没有数据源的查询权限]");
                    }
                    List<DatasourceVersion> versions =
                            dataSourceInfoService.getVersionList(dataSourceId);
                    // Decrypt
                    if (null != versions) {
                        versions.forEach(
                                version -> {
                                    RestfulApiHelper.decryptPasswordKey(
                                            dataSourceRelateService.getKeyDefinitionsByType(
                                                    dataSource.getDataSourceTypeId()),
                                            version.getConnectParams());
                                });
                    }
                    return Message.ok().data("versions", versions);
                },
                "Fail to access data source[获取数据源信息失败]");
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public Message publishByDataSourceId(
            @RequestParam("dataSourceId") Long dataSourceId,
            @RequestParam("versionId") Long versionId,
            HttpServletRequest request) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    // Get brief info
                    DataSource dataSource =
                            dataSourceInfoService.getDataSourceInfoBrief(dataSourceId);

                    if (dataSource == null) {
                        return Message.error("No Exists The DataSource [不存在该数据源]");
                    }

                    if (!AuthContext.hasPermission(dataSource, request)) {
                        return Message.error(
                                "Don't have publish permission for data source [没有数据源的发布权限]");
                    }
                    int updateResult =
                            dataSourceInfoService.publishByDataSourceId(dataSourceId, versionId);
                    if (0 == updateResult) {
                        return Message.error("publish error");
                    }
                    return Message.ok();
                },
                "Fail to publish datasource[数据源版本发布失败]");
    }



    /**
     * get datasource connect params for publish version
     *
     * @param dataSourceId
     * @param req
     * @return
     */
    @RequestMapping(value = "/getConnectParams", method = RequestMethod.GET)
    public Message getConnectParams(
            @RequestParam("dataSourceId") Long dataSourceId, HttpServletRequest req) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    DataSource dataSource =
                            dataSourceInfoService.getDataSourceInfoForConnect(dataSourceId);

                    if (dataSource == null) {
                        return Message.error("No Exists The DataSource [不存在该数据源]");
                    }

                    if (!AuthContext.hasPermission(dataSource, req)) {
                        return Message.error(
                                "Don't have query permission for data source [没有数据源的查询权限]");
                    }
                    Map<String, Object> connectParams = dataSource.getConnectParams();
                    RestfulApiHelper.decryptPasswordKey(
                            dataSourceRelateService.getKeyDefinitionsByType(
                                    dataSource.getDataSourceTypeId()),
                            connectParams);
                    return Message.ok().data("connectParams", connectParams);
                },
                "Fail to connect data source[连接数据源失败]");
    }

    @RequestMapping(value = "/getConnectParamsByName", method = RequestMethod.GET)
    public Message getConnectParams(
            @RequestParam("dataSourceName") String dataSourceName, HttpServletRequest req)
            throws UnsupportedEncodingException {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    DataSource dataSource =
                            dataSourceInfoService.getDataSourceInfoForConnect(dataSourceName);

                    if (dataSource == null) {
                        return Message.error("No Exists The DataSource [不存在该数据源]");
                    }

                    if (!AuthContext.hasPermission(dataSource, req)) {
                        return Message.error(
                                "Don't have query permission for data source [没有数据源的查询权限]");
                    }
                    Map<String, Object> connectParams = dataSource.getConnectParams();
                    RestfulApiHelper.decryptPasswordKey(
                            dataSourceRelateService.getKeyDefinitionsByType(
                                    dataSource.getDataSourceTypeId()),
                            connectParams);
                    return Message.ok().data("connectParams", connectParams);
                },
                "Fail to connect data source[连接数据源失败]");
    }



    /**
     * Inner method to insert data source
     *
     * @param dataSource data source entity
     * @throws ParameterValidateException
     */
    private void insertDataSource(DataSource dataSource) throws ErrorException {
        List<DataSourceParamKeyDefinition> keyDefinitionList =
                dataSourceRelateService.getKeyDefinitionsByType(dataSource.getDataSourceTypeId());
        dataSource.setKeyDefinitions(keyDefinitionList);
        dataSourceInfoService.saveDataSourceInfo(dataSource);
    }
}
