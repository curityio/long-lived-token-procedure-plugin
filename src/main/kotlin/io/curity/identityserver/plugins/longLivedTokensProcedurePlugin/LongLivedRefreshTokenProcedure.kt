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

import se.curity.identityserver.sdk.attribute.Attribute
import se.curity.identityserver.sdk.attribute.token.AccessTokenAttributes
import se.curity.identityserver.sdk.attribute.token.IdTokenAttributes
import se.curity.identityserver.sdk.procedure.token.RefreshTokenProcedure
import se.curity.identityserver.sdk.procedure.token.context.RefreshTokenProcedurePluginContext
import se.curity.identityserver.sdk.web.ResponseModel
import java.lang.RuntimeException
import java.time.Duration
import java.time.Instant

class LongLivedRefreshTokenProcedure(private val configuration: LongLivedTokensProcedurePluginConfig): RefreshTokenProcedure
{
    override fun run(pluginContext: RefreshTokenProcedurePluginContext): ResponseModel
    {
        val shouldIssueIDToken = pluginContext.client.typedProperties["id_token_on_refresh"] == "true"

        val shouldIssueLongLivedTokenParameter = pluginContext.request.getQueryParameterValueOrError("long_lived_token")
            { throw RuntimeException("More than one value of long_lived_token parameter found.") }

        val accessTokenData = pluginContext.defaultAccessTokenData

        val finalAccessTokenData = if (shouldIssueLongLivedTokenParameter != null && shouldIssueLongLivedTokenParameter == "true") {
            val accessTokenDataMap = accessTokenData.asMap()
            accessTokenDataMap["exp"] = Instant.now().plusSeconds(configuration.getLongLivedAccessTokenExpiration()).epochSecond
            AccessTokenAttributes.fromMap(accessTokenDataMap)
        } else {
            accessTokenData
        }

        val accessToken = pluginContext.accessTokenIssuer.issue(finalAccessTokenData, pluginContext.delegation)
        val responseMap = mutableMapOf<String, Any>(
            "scope" to pluginContext.scope,
            "access_token" to accessToken,
            "token_type" to "bearer",
            "expires_in" to Duration.ofSeconds(finalAccessTokenData.expires.epochSecond - Instant.now().epochSecond).seconds,
            "refresh_token" to pluginContext.refreshTokenIssuer.issue(pluginContext.defaultRefreshTokenData, pluginContext.delegation)
        )

        if (shouldIssueIDToken) {
            val idTokenIssuer = pluginContext.idTokenIssuer
            val idTokenData = pluginContext
                .getDefaultData("id_token")
                .with(Attribute.of("at_hash", idTokenIssuer.atHash(accessToken)))
            responseMap["id_token"] = idTokenIssuer.issue(IdTokenAttributes.of(idTokenData))
        }

        return ResponseModel.mapResponseModel(responseMap)
    }
}
