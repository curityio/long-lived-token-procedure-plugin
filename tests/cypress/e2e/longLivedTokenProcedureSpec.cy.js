/*
 * Copyright 2023 Curity AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { registerCurityCommands } from "@curity/cypress-commands"

registerCurityCommands()

const baseURL = 'https://localhost:8443'

const authorizationURL = baseURL + '/oauth/v2/oauth-authorize'
const tokenEndpointURL = baseURL + '/oauth/v2/oauth-token'
const introspectionEndpointURL = baseURL + '/oauth/v2/oauth-introspect'

const clientRedirectURI = 'http://localhost/cb'
const clientID = 'client-without-parameter'
const clientSecret = 'Password1'

const parameters = {
    baseURL: authorizationURL,
    clientID: clientID,
    redirectURI: clientRedirectURI,
    responseType: 'code',
    scope: 'openid'
}

beforeEach(() => {
    // Intercept the redirect URI callback and return a mock 200 response so that cypress does not fail the test.
    cy.intercept({
        pathname: '/cb'
    }, (req) => {
        req.reply(200, {message: 'Response from the callback'})
    })
})

describe('Long-Lived Token Procedure tests', () => {
    it('A refreshed access token should have a default short TTL', () => {
        login(parameters)

        // Get the access token
        cy.url().then(urlString => {
            const url = new URL(urlString)
            const code = url.searchParams.get('code')

            exchangeCodeForTokens(code, clientID).then(tokenResponse => {
                refreshToken(tokenResponse.body.refresh_token, clientID).then(refreshResponse => {
                    const refreshedAccessToken = refreshResponse.body.access_token

                    expect(refreshResponse.body.expires_in).to.be.approximately(300, 2)

                    introspect(refreshedAccessToken).then(tokenData => {
                        expect(tokenData.exp).to.be.approximately(now() + 300, 2)
                    })
                })
            })
        })
    })

    it('When query parameter is used in the refresh token procedure, the access token should have a long TTL', () => {
        login(parameters)

        // Get the access token
        cy.url().then(urlString => {
            const url = new URL(urlString)
            const code = url.searchParams.get('code')

            exchangeCodeForTokens(code, clientID).then(tokenResponse => {

                refreshToken(tokenResponse.body.refresh_token, clientID, 'long_lived_token=true').then(refreshResponse => {

                    expect(refreshResponse.body.expires_in).to.be.approximately(14400, 2)

                    introspect(refreshResponse.body.access_token).then(tokenData => {
                        expect(tokenData.exp).to.be.approximately(now() + 14400, 2)
                    })
                })
            })
        })
    })

    it('If client has the id_token_on_refresh property, then ID token should be returned on refresh in an OIDC flow', () => {
        const clientID = 'client-with-parameter'

        const authorizationParameters = {
            ...parameters,
            clientID: clientID
        }

        login(authorizationParameters)

        // Get the access token
        cy.url().then(urlString => {
            const url = new URL(urlString)
            const code = url.searchParams.get('code')

            exchangeCodeForTokens(code, clientID).then(tokenResponse => {
                refreshToken(tokenResponse.body.refresh_token, clientID).then(refreshResponse => {
                    expect(refreshResponse.body.id_token).to.exist
                })
            })
        })
    })

    it('The ID token should not be returned even if client has id_token_on_refresh if OIDC is not used', () => {
        const clientID = 'client-with-parameter'

        const authorizationParameters = {
            ...parameters,
            clientID: clientID,
            scope: ''
        }
        login(authorizationParameters)

        // Get the access token
        cy.url().then(urlString => {
            const url = new URL(urlString)
            const code = url.searchParams.get('code')

            exchangeCodeForTokens(code, clientID).then(tokenResponse => {
                refreshToken(tokenResponse.body.refresh_token, clientID).then(refreshResponse => {
                    expect(refreshResponse.body.id_token).to.not.exist
                })
            })
        })
    })
})

const introspect = (accessToken) => {
    return cy.request({
        url: introspectionEndpointURL,
        method: 'POST',
        auth: { username: clientID, password: clientSecret },
        body: { token: accessToken },
        form: true
    }).then(response => {
        expect(response.status).to.equal(200)
        return response.body
    })
}

const now = () => Date.now() / 1000

const refreshToken = (refreshToken, clientID, queryString) => cy.request({
    auth: { username: clientID, password: clientSecret },
    method: 'POST',
    url: tokenEndpointURL + (queryString ? '?' + queryString : ''),
    body: {
        refresh_token: refreshToken,
        grant_type: 'refresh_token'
    },
    form: true
})

const exchangeCodeForTokens = (code, clientID) => cy.request({
    body: {
        code,
        grant_type: 'authorization_code',
        redirect_uri: clientRedirectURI
    },
    auth: {
        user: clientID,
        pass: clientSecret
    },
    headers: {
        'content-type': 'application/x-www-form-urlencoded'
    },
    method: 'POST',
    url: tokenEndpointURL
})

const login = (parameters) => {
    // Start the authorization flow
    cy.startAuthorization(parameters)

    // Enter username and click "Next"
    cy.get('#username').type("test")
    cy.get('button[type=submit]').click()
}
