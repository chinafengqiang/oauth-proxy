
/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.proxy.bean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "error_code", "error_type", "error_message" })
@XmlRootElement(name = "error_response")
public class ErrorResponse {

    @XmlAttribute(name = "error_code")
    @SerializedName("error_code")
    private String code;
    @SerializedName("error_type")
    @XmlAttribute(name = "error_type")
    private String type;
    @SerializedName("error_message")
    @XmlAttribute(name = "error_message")
    private String message;

    private static final Gson gson = new Gson();

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String type, String message) {
        this.code = code;
        this.type = type;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static ErrorResponse buildFromJson(String json) {
        return gson.fromJson(json, ErrorResponse.class);
    }
}