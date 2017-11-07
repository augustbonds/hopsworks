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
package io.hops.hopsworks.apiV2.users

import com.fasterxml.jackson.annotation.JsonProperty
import io.hops.hopsworks.common.dao.user.Users

class UserRest (
        @param:JsonProperty("firstname") val firstname: String,
        @param:JsonProperty("lastname") val lastname: String,
        @param:JsonProperty("uid") val uid: Int
)

fun fromUsers(user: Users): UserRest {
    return UserRest(user.fname, user.lname, user.uid)
}