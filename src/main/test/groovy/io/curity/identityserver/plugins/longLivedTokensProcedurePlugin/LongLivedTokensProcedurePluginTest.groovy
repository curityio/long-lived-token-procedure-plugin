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

import se.curity.identityserver.sdk.attribute.Attributes
import se.curity.identityserver.sdk.attribute.token.AccessTokenAttributes
import se.curity.identityserver.sdk.attribute.token.IdTokenAttributes
import se.curity.identityserver.sdk.attribute.token.RefreshTokenAttributes
import se.curity.identityserver.sdk.data.authorization.Delegation
import se.curity.identityserver.sdk.oauth.OAuthClient
import se.curity.identityserver.sdk.procedure.token.context.RefreshTokenProcedurePluginContext
import se.curity.identityserver.sdk.service.issuer.AccessTokenIssuer
import se.curity.identityserver.sdk.service.issuer.IdTokenIssuer
import se.curity.identityserver.sdk.service.issuer.RefreshTokenIssuer
import se.curity.identityserver.sdk.web.Request
import spock.lang.Specification

import java.time.Instant

class LongLivedTokensProcedurePluginTest extends Specification {

    def now = Instant.now().epochSecond
    def exp = Instant.now().plusSeconds(10).epochSecond
    def accessTokenData = AccessTokenAttributes.of(Attributes.fromMap([
            "iat": now,
            "nbf": now,
            "exp": exp,
            "iss": "",
            "sub": "",
            "purpose": "access_token",
            "scope": ""
    ]))

    def idTokenData = Attributes.fromMap([
            "jti": "123",
            "iss": "issuer",
            "sub": "user",
            "aud": "client_id",
            "nbf": now,
            "auth_time": now,
            "exp": exp,
            "iat": now,
            "purpose": "id"

    ])

    def delegation = Stub(Delegation)
    def idTokenAttributes
    def idTokenIssuer = Mock(IdTokenIssuer)
    def accessTokenIssuer = Mock(AccessTokenIssuer)
    def configuration = Mock(LongLivedTokensProcedurePluginConfig)
    def context = Mock(RefreshTokenProcedurePluginContext)
    def refreshTokenIssuer = Mock(RefreshTokenIssuer)
    def refreshTokenData

    def setup() {
        context.accessTokenIssuer >> accessTokenIssuer
        context.idTokenIssuer >> idTokenIssuer

        idTokenAttributes = GroovyMock(type: IdTokenAttributes, global: true)
        refreshTokenData = GroovyMock(type: RefreshTokenAttributes, global: true)

        refreshTokenIssuer.issue(_, _) >> "refresh_token"

        context.delegation >> delegation
        context.defaultIdTokenData >> idTokenAttributes
        context.defaultAccessTokenData >> accessTokenData
        context.defaultRefreshTokenData >> refreshTokenData
        context.refreshTokenIssuer >> refreshTokenIssuer
        context.getDefaultData("id_token") >> idTokenData

        configuration.longLivedAccessTokenExpiration >> 2 * 60 * 60
    }

    def "shouldIssueIDTokenWithAtHashWhenClientPropertySetOnClient"() {
        given:
        def plugin = new LongLivedRefreshTokenProcedure(configuration)

        def client = Mock(OAuthClient)
        client.getTypedProperties() >> ["id_token_on_refresh": "true"]
        context.client >> client
        context.scope >> "openid"

        def request = Mock(Request)
        request.getQueryParameterValueOrError("long_lived_token", _) >> null
        context.request >> request

        context.accessTokenIssuer.issue(_, _) >> "access_token"

        when:
        def response = plugin.run(context)

        then:
        response.viewData.containsKey("id_token")
        1 * idTokenIssuer.atHash("access_token") >> "at_hash"
        1 * idTokenIssuer.issue(_) >> "id_token_value"
    }

    def "shouldNotIssueIDTokenForClientsWithoutTheParameter"() {
        given:
        def plugin = new LongLivedRefreshTokenProcedure(configuration)

        def client = Mock(OAuthClient)
        client.getTypedProperties() >> Map.of()
        context.client >> client
        context.scope >> "openid"

        def request = Mock(Request)
        request.getQueryParameterValueOrError("long_lived_token", _) >> null
        context.request >> request

        when:
        def response = plugin.run(context)

        then:
        !response.viewData.containsKey("id_token")
        0 * idTokenIssuer.issue(_)
    }

    def "shouldNotIssueIDTokenWhenClientPropertySetOnClientButNoOIDCUsed"() {
        given:
        def plugin = new LongLivedRefreshTokenProcedure(configuration)

        def client = Mock(OAuthClient)
        client.getTypedProperties() >> ["id_token_on_refresh": "true"]
        context.client >> client
        context.scope >> "read"

        def request = Mock(Request)
        request.getQueryParameterValueOrError("long_lived_token", _) >> null
        context.request >> request

        context.accessTokenIssuer.issue(_, _) >> "access_token"

        when:
        def response = plugin.run(context)

        then:
        !response.viewData.containsKey("id_token")
        0 * idTokenIssuer.issue(_)
    }

    def "shouldIssueLongLivedAccessTokenWhenParameterSetToTrue"() {
        given:
        def plugin = new LongLivedRefreshTokenProcedure(configuration)

        def client = Mock(OAuthClient)
        client.getTypedProperties() >> Map.of()
        context.client >> client
        context.scope >> "openid"

        def request = Mock(Request)
        request.getQueryParameterValueOrError("long_lived_token", _) >> "true"
        context.request >> request

        def expiresIn = 2 * 60 * 60
        def expiration = Instant.now().plusSeconds(expiresIn).epochSecond

        when:
        def response = plugin.run(context)

        then:
        response.viewData.get("expires_in") == expiresIn
        1 * accessTokenIssuer.issue((AccessTokenAttributes tokenData) -> { tokenData.expires.epochSecond == expiration }, _) >> "long_lived_access_token"
    }

    def "shouldIssueShortLivedAccessTokenWhenParameterSetToTrue"() {
        given:
        def plugin = new LongLivedRefreshTokenProcedure(configuration)

        def client = Stub(OAuthClient)
        client.getTypedProperties() >> Map.of()
        context.client >> client
        context.scope >> "openid"

        def request = Stub(Request)
        request.getQueryParameterValueOrError("long_lived_token", _) >> null
        context.request >> request

        def expiresIn = 10
        def expiration = Instant.now().plusSeconds(expiresIn).epochSecond

        when:
        def response = plugin.run(context)

        then:
        response.viewData.get("expires_in") == expiresIn
        1 * accessTokenIssuer.issue((AccessTokenAttributes tokenData) -> { tokenData.expires.epochSecond == expiration }, _) >> "short_lived_access_token"
    }
}
