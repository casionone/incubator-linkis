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

package org.apache.linkis.cs.server.restful;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.linkis.server.Message;
import org.apache.linkis.common.conf.Configuration;
import org.apache.linkis.cs.common.entity.enumeration.ContextType;
import org.apache.linkis.cs.common.entity.source.ContextID;
import org.apache.linkis.cs.common.entity.source.ContextKey;
import org.apache.linkis.cs.common.entity.source.ContextKeyValue;
import org.apache.linkis.cs.common.entity.source.ContextValue;
import org.apache.linkis.cs.common.exception.CSErrorException;
import org.apache.linkis.cs.common.protocol.ContextHTTPConstant;
import org.apache.linkis.cs.common.utils.CSCommonUtils;
import org.apache.linkis.cs.server.enumeration.ServiceMethod;
import org.apache.linkis.cs.server.enumeration.ServiceType;
import org.apache.linkis.cs.server.scheduler.CsScheduler;
import org.apache.linkis.cs.server.scheduler.HttpAnswerJob;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.utils.ModuleUserUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.linkis.cs.common.utils.CSCommonUtils.localDatetimeToDate;
@Api(tags = "上下文服务")
@RestController
@RequestMapping(path = "/contextservice")
public class ContextRestfulApi implements CsRestfulParent {

    private static final Logger logger = LoggerFactory.getLogger(ContextRestfulApi.class);

    @Autowired private CsScheduler csScheduler;

    private ObjectMapper objectMapper = new ObjectMapper();


    @ApiOperation(value="获取上下文内容",notes="获取上下文内容",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="contextKey",dataType="String",value="contextKey"),
            @ApiImplicitParam(name="contextID",dataType="String",value="上下文id")
    })
    @RequestMapping(path = "getContextValue", method = RequestMethod.POST)
    public Message getContextValue(HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        ContextID contextID = getContextIDFromJsonNode(jsonNode);
        ContextKey contextKey = getContextKeyFromJsonNode(jsonNode);
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.GET, contextID, contextKey);
        Message message = generateResponse(answerJob, "contextValue");
        return message;
    }
    @ApiOperation(value="搜索上下文内容",notes="搜索上下文内容",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="condition",dataType="String",value="condition"),
            @ApiImplicitParam(name="contextID",dataType="String",value="上下文id")
    })
    @RequestMapping(path = "searchContextValue", method = RequestMethod.POST)
    public Message searchContextValue(HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        ContextID contextID = getContextIDFromJsonNode(jsonNode);
        JsonNode condition = jsonNode.get("condition");
        Map<Object, Object> conditionMap =
                objectMapper.convertValue(condition, new TypeReference<Map<Object, Object>>() {});
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.SEARCH, contextID, conditionMap);
        Message message = generateResponse(answerJob, "contextKeyValue");
        return message;
    }

    /*
    @RequestMapping(path = "searchContextValueByCondition",method = RequestMethod.GET)
    public Message searchContextValueByCondition(HttpServletRequest req, JsonNode jsonNode) throws InterruptedException {
        Condition condition = null;
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.SEARCH, condition);
        return generateResponse(answerJob,"");
    }*/
    @ApiOperation(value="设置key",notes="给value设置key",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="contextKey",dataType="String",value="contextKey"),
            @ApiImplicitParam(name="contextID",dataType="String",value="上下文id")
    })
    @RequestMapping(path = "setValueByKey", method = RequestMethod.POST)
    public Message setValueByKey(HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws CSErrorException, IOException, ClassNotFoundException, InterruptedException {
        ContextID contextID = getContextIDFromJsonNode(jsonNode);
        ContextKey contextKey = getContextKeyFromJsonNode(jsonNode);
        ContextValue contextValue = getContextValueFromJsonNode(jsonNode);
        HttpAnswerJob answerJob =
                submitRestJob(req, ServiceMethod.SET, contextID, contextKey, contextValue);
        return generateResponse(answerJob, "");
    }
    @ApiOperation(value="设置value",notes="设置value",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="contextKeyValue",dataType="String",value="contextKeyValue"),
            @ApiImplicitParam(name="contextID",dataType="String",value="上下文id")
    })
    @RequestMapping(path = "setValue", method = RequestMethod.POST)
    public Message setValue(HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        ContextID contextID = getContextIDFromJsonNode(jsonNode);
        ContextKeyValue contextKeyValue = getContextKeyValueFromJsonNode(jsonNode);
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.SET, contextID, contextKeyValue);
        return generateResponse(answerJob, "");
    }
    @ApiOperation(value="重置value",notes="重置value",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="contextKey",dataType="String",value="contextKey"),
            @ApiImplicitParam(name="contextID",dataType="String",value="上下文id")
    })
    @RequestMapping(path = "resetValue", method = RequestMethod.POST)
    public Message resetValue(HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        ContextID contextID = getContextIDFromJsonNode(jsonNode);
        ContextKey contextKey = getContextKeyFromJsonNode(jsonNode);
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.RESET, contextID, contextKey);
        return generateResponse(answerJob, "");
    }
    @ApiOperation(value="删除value",notes="删除value",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="contextKey",dataType="String",value="contextKey"),
            @ApiImplicitParam(name="contextID",dataType="String",value="上下文id")
    })
    @RequestMapping(path = "removeValue", method = RequestMethod.POST)
    public Message removeValue(HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        ContextID contextID = getContextIDFromJsonNode(jsonNode);
        ContextKey contextKey = getContextKeyFromJsonNode(jsonNode);
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.REMOVE, contextID, contextKey);
        return generateResponse(answerJob, "");
    }
    @ApiOperation(value="删除所有value",notes="删除所有value",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="contextKey",dataType="String",value="contextKey"),
            @ApiImplicitParam(name="contextID",dataType="String",value="上下文id")
    })
    @RequestMapping(path = "removeAllValue", method = RequestMethod.POST)
    public Message removeAllValue(HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        ContextID contextID = getContextIDFromJsonNode(jsonNode);
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.REMOVEALL, contextID);
        return generateResponse(answerJob, "");
    }
    @ApiOperation(value="通过key前缀和上下文类型删除所有值",notes="通过前缀和上下文类型删除所有值",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="contextKeyType",dataType="String",value="contextKeyType"),
            @ApiImplicitParam(name="keyPrefix",dataType="String",value="keyPrefix")
    })
    @RequestMapping(path = "removeAllValueByKeyPrefixAndContextType", method = RequestMethod.POST)
    public Message removeAllValueByKeyPrefixAndContextType(
            HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        ContextID contextID = getContextIDFromJsonNode(jsonNode);
        String contextType = jsonNode.get(ContextHTTPConstant.CONTEXT_KEY_TYPE_STR).textValue();
        String keyPrefix = jsonNode.get(ContextHTTPConstant.CONTEXT_KEY_PREFIX_STR).textValue();
        HttpAnswerJob answerJob =
                submitRestJob(
                        req,
                        ServiceMethod.REMOVEALL,
                        contextID,
                        ContextType.valueOf(contextType),
                        keyPrefix);
        return generateResponse(answerJob, "");
    }
    @ApiOperation(value="通过value前缀删除所有值",notes="通过前缀和上下文类型删除所有值",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="contextID",dataType="String",value="contextID"),
            @ApiImplicitParam(name="keyPrefix",dataType="String",value="keyPrefix")
    })
    @RequestMapping(path = "removeAllValueByKeyPrefix", method = RequestMethod.POST)
    public Message removeAllValueByKeyPrefix(HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        ContextID contextID = getContextIDFromJsonNode(jsonNode);
        String keyPrefix = jsonNode.get(ContextHTTPConstant.CONTEXT_KEY_PREFIX_STR).textValue();
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.REMOVEALL, contextID, keyPrefix);
        return generateResponse(answerJob, "");
    }
    @ApiOperation(value="通过ID清除所以上下文",notes="通过ID清除所以上下文",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="idList",dataType="String",value="上下文id集合"),
    })
    @RequestMapping(path = "clearAllContextByID", method = RequestMethod.POST)
    public Message clearAllContextByID(HttpServletRequest req, @RequestBody JsonNode jsonNode)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        if (null == jsonNode
                || !jsonNode.has("idList")
                || !jsonNode.get("idList").isArray()
                || (jsonNode.get("idList").isArray()
                        && ((ArrayNode) jsonNode.get("idList")).size() == 0)) {
            throw new CSErrorException(97000, "idList cannot be empty.");
        }
        ArrayNode idArray = (ArrayNode) jsonNode.get("idList");
        logger.info("clearAllContextByID idList size : {}", idArray.size());
        List<String> idList = new ArrayList<>(idArray.size());
        for (int i = 0; i < idArray.size(); i++) {
            idList.add(idArray.get(i).asText());
        }
        HttpAnswerJob answerJob = submitRestJob(req, ServiceMethod.CLEAR, idList);
        Message resp = generateResponse(answerJob, "num");
        resp.setMethod("/api/contextservice/clearAllContextByID");
        return resp;
    }

    @ApiOperation(value="通过时间清除所以上下文",notes="通过时间清除所以上下文",response = Message.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name="createTimeStart",dataType="String",value="创建时间"),
            @ApiImplicitParam(name="createTimeEnd",dataType="String",value="创建时间结束"),
            @ApiImplicitParam(name="updateTimeStart",dataType="String",value="更新开始时间"),
            @ApiImplicitParam(name="createTimeEnd",dataType="String",value="更新结束时间"),
            @ApiImplicitParam(name="accessTimeStart",dataType="String",value="访问时间开始"),
            @ApiImplicitParam(name="accessTimeEnd",dataType="String",value="访问时间结束")
    })
    @RequestMapping(path = "clearAllContextByTime", method = RequestMethod.POST)
    public Message clearAllContextByID(
            HttpServletRequest req, @RequestBody Map<String, Object> bodyMap)
            throws InterruptedException, CSErrorException, IOException, ClassNotFoundException {
        String username = ModuleUserUtils.getOperationUser(req);
        if (!Configuration.isAdmin(username)) {
            throw new CSErrorException(97018, "Only station admins are allowed.");
        }
        if (null == bodyMap || bodyMap.isEmpty()) {
            throw new CSErrorException(97000, "idList cannot be empty.");
        }
        Date createTimeStart = null;
        Date createTimeEnd = null;
        Date updateTimeStart = null;
        Date updateTimeEnd = null;
        Date accessTimeStart = null;
        Date accessTimeEnd = null;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(CSCommonUtils.DEFAULT_TIME_FORMAT);
        if (bodyMap.containsKey("createTimeStart") && null != bodyMap.get("createTimeStart"))
            createTimeStart =
                    localDatetimeToDate(
                            LocalDateTime.parse((String) bodyMap.get("createTimeStart"), dtf));
        if (bodyMap.containsKey("createTimeEnd") && null != bodyMap.get("createTimeEnd"))
            createTimeEnd =
                    localDatetimeToDate(
                            LocalDateTime.parse((String) bodyMap.get("createTimeEnd"), dtf));
        if (bodyMap.containsKey("updateTimeStart") && null != bodyMap.get("updateTimeStart"))
            updateTimeStart =
                    localDatetimeToDate(
                            LocalDateTime.parse((String) bodyMap.get("updateTimeStart"), dtf));
        if (bodyMap.containsKey("updateTimeEnd") && null != bodyMap.get("updateTimeEnd"))
            updateTimeEnd =
                    localDatetimeToDate(
                            LocalDateTime.parse((String) bodyMap.get("updateTimeEnd"), dtf));
        if (bodyMap.containsKey("accessTimeStart") && null != bodyMap.get("accessTimeStart"))
            updateTimeStart =
                    localDatetimeToDate(
                            LocalDateTime.parse((String) bodyMap.get("accessTimeStart"), dtf));
        if (bodyMap.containsKey("accessTimeEnd") && null != bodyMap.get("accessTimeEnd"))
            updateTimeEnd =
                    localDatetimeToDate(
                            LocalDateTime.parse((String) bodyMap.get("accessTimeEnd"), dtf));
        if (null == createTimeStart
                && null == createTimeEnd
                && null == updateTimeStart
                && null == createTimeEnd) {
            throw new CSErrorException(
                    97000,
                    "createTimeStart, createTimeEnd, updateTimeStart, updateTimeEnd cannot be all null.");
        }
        logger.info(
                "clearAllContextByTime: user : {}, createTimeStart : {}, createTimeEnd : {}, updateTimeStart : {}, updateTimeEnd : {}, accessTimeStart : {}, accessTimeEnd : {}, pageNow : {}, pageSize : {}.",
                username,
                createTimeStart,
                createTimeEnd,
                updateTimeStart,
                updateTimeEnd,
                accessTimeStart,
                accessTimeEnd);
        HttpAnswerJob answerJob =
                submitRestJob(
                        req,
                        ServiceMethod.CLEAR,
                        createTimeStart,
                        createTimeEnd,
                        updateTimeStart,
                        updateTimeEnd,
                        accessTimeStart,
                        accessTimeEnd);
        Message resp = generateResponse(answerJob, "num");
        resp.setMethod("/api/contextservice/clearAllContextByTime");
        return resp;
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.CONTEXT;
    }

    @Override
    public CsScheduler getScheduler() {
        return this.csScheduler;
    }
}
