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
import org.apache.linkis.datasourcemanager.common.domain.DataSourceType;
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
import org.apache.linkis.metadata.query.common.MdmConfiguration;
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
@RequestMapping(value = "/data-source-manager/type")
public class DataSourceTypeRestfulApi {

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceTypeRestfulApi.class);

    @Autowired private DataSourceRelateService dataSourceRelateService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Message getAllDataSourceTypes() {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    List<DataSourceType> dataSourceTypes =
                            dataSourceRelateService.getAllDataSourceTypes();
                    return Message.ok().data("typeList", dataSourceTypes);
                },
                "Fail to get all types of data source[获取数据源类型列表失败]");
    }

    @RequestMapping(value = "/getKeysByName", method = RequestMethod.GET)
    public Message getKeyDefinitionsByType(@RequestParam("name") String name) {
        return RestfulApiHelper.doAndResponse(
                () -> {
                    List<DataSourceParamKeyDefinition> keyDefinitions =
                            dataSourceRelateService.getKeyDefinitionsByTypeName(name);
                    return Message.ok().data("keyDefine", keyDefinitions);
                },
                "Fail to get key definitions of data source type[查询数据源参数键值对失败]");
    }


}
