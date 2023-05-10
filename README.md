# Long-Lived Tokens Procedure Plugin

[![Quality](https://img.shields.io/badge/quality-test-yellow)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

A token procedure plugin that adds custom behavior to the refresh token endpoint.

The plugin adds the following two features to the refresh token endpoint:
- If the request to the endpoint contains the query parameter `long_lived_token` with value set to `true`, then the new access token is issued for a duration set in the plugin's configuration, instead of the value set for the given client.
- If the client has a property `id_token_on_refresh` set to `true`, then an ID token will be issued together with the response to the refresh token request.

## Building the Plugin

Build the plugin by issuing the command `mvn package`. This will produce a JAR file in the `target` directory, which can be installed.

## Installing the Plugin

To install the plugin after building, copy the contents of `/target/` to `${IDSVR_HOME}/usr/share/plugins/longlivedtokenprocedure`, on each node of
the Curity Identity Server, including the admin node.

For more information about installing plugins, refer to the [curity.io/plugins](https://support.curity.io/docs/latest/developer-guide/plugins/index.html#plugin-installation).

## Required Dependencies

For a list of the dependencies and their versions, run `mvn dependency:list`.

If you modify this plugin and add any `runtime` or `compile` scope dependencies, then ensure that all of these are installed in the plugin group. Otherwise, they will not be accessible to this plug-in and run-time errors will result.

## Configuring the Plugin

The plugin has the following configuration options:

- `Long-Lived Access Token Expiration` — the Time To Live for a long-lived access token, in seconds. Defaults to 4 hours.
- `Access Token Issuer` — a custom access token issuer. The default issuer is used if none provided.
- `Refresh Token Issuer` — a custom refresh token issuer. The default issuer is used if none provided.
- `ID Token Issuer` — a custom ID token issuer. The default issuer is used if none provided.

## Enabling the Plugin

To enable the plugin using the admin UI go to your **Token Service** profile, then **Endpoints**. Locate the endpoint with type `oauth-token` and click on the `Flows` dropdown. Click on the dropdown under the `Refresh` flow and select **+ New Plugin**.

![Enable the plugin](/docs/enable-plugin.jpg)

Give the plugin a name, and select the "Long Lived Token on Refresh" plugin tile.

![New plugin](/docs/new-plugin.jpg)

You can then change the configuration option for the plugin. Once this is adjusted, commit the changes. The plugin will now be run when you call the refresh token endpoint.

![Edit plugin](/docs/edit-plugin.jpg)

You can manage your token procedure plugins by going to the **System** tab, then choosing **Token Procedure Plugins** from the sidebar menu.

## Running Tests

See [TEST.md](/TEST.md) for options on running the test suite for this plugin.

## More Information

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.
