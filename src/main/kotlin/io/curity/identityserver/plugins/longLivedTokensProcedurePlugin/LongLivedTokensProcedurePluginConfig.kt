/*
 *  Copyright 2023 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.curity.identityserver.plugins.longLivedTokensProcedurePlugin

import se.curity.identityserver.sdk.config.Configuration
import se.curity.identityserver.sdk.config.annotation.DefaultLong
import se.curity.identityserver.sdk.config.annotation.DefaultService
import se.curity.identityserver.sdk.config.annotation.Description
import se.curity.identityserver.sdk.service.issuer.AccessTokenIssuer
import se.curity.identityserver.sdk.service.issuer.IdTokenIssuer
import se.curity.identityserver.sdk.service.issuer.RefreshTokenIssuer

interface LongLivedTokensProcedurePluginConfig: Configuration
{
    @Description("The Time To Live for a long-lived access token, in seconds. Defaults to 4 hours.")
    @DefaultLong(4 * 60 * 60)
    fun getLongLivedAccessTokenExpiration(): Long

    @DefaultService
    @Description("A custom access token issuer to be used in the procedure. The default issuer is used if none provided.")
    fun getAccessTokenIssuer(): AccessTokenIssuer

    @DefaultService
    @Description("A custom refresh token issuer to be used in the procedure. The default issuer is used if none provided.")
    fun getRefreshTokenIssuer(): RefreshTokenIssuer

    @DefaultService
    @Description("A custom ID token issuer to be used in the procedure. The default issuer is used if none provided.")
    fun getIdTokenIssuer(): IdTokenIssuer
}
