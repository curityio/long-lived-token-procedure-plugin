# Running Tests

This repository is accompanied by tests, which can be run locally using the following instructions.

## Spock Unit Tests

During development, run unit tests that mock services of the Curity Identity Server to make sure that the procedure returns expected results.

```bash
mvn test
```

## Cypress Integration Tests

These are end-to-end tests that use a real instance of the Curity Identity Server and a browser. \
To run them locally you will need a license for the Curity Identity Server. Put the license JWT in a `TEST_LICENSE`\
environment variable. Build the plugin jar and move it to a `/plugin` directory, then spin up the Curity Identity Server:

```bash
export TEST_LICENSE=eyJ...Uuw
mvn package
mv target/*.jar plugin/
docker run -d --rm -e PASSWORD=Password1 -e TEST_LICENSE=$TEST_LICENSE \
-v $PWD/plugin:/opt/idsvr/usr/share/plugins/long-lived-token-procedure \
-v $PWD/tests/idsvr/config.xml:/opt/idsvr/etc/init/config.xml \
-p 6749:6749 -p 8443:8443 curity.azurecr.io/curity/idsvr:latest
```

Once the server has started, open the Cypress console to run the browser tests:

```bash
cd tests
npm install
npm run cypress.open
```

You can then easily study the results of the tests:

![Cypress test results](/docs/cypress-results.jpg)

To run this test suite using a headless browser, issue the following command:

```bash
npm run cypress.run
```
