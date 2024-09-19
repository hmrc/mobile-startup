# mobile-startup

Provide the mobile apps with information they need at the time that they are launched.

(This service replaced functionality that used to be provided by `native-apps-api-orchestration`)

## Development Setup
- Run locally: `sbt run` which runs on port `8251` by default
- Run with test endpoints: `sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes'`

##  Service Manager Profiles
The service can be run locally from Service Manager, using the following profiles:

| Profile Details               | Command                                                                                                           |
|-------------------------------|:------------------------------------------------------------------------------------------------------------------|
| MOBILE_STARTUP_ALL            | sm2 --start MOBILE_STARTUP_ALL --appendArgs '{"MOBILE_STARTUP": ["-Dfeature.annualTaxSummaryLink='true'"]}'                                                                    |


## Run Tests
- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it:test`
- Run Unit and Integration Tests: `sbt test it:test`
- Run Unit and Integration Tests with coverage report: `sbt clean compile coverage test it:test coverageReport dependencyUpdates`


The following services are exposed:

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/preflight-check``` | GET | Returns account identifiers using the auth record of the user. [More...](docs/preflight-check.md) |
| ```/startup``` | GET | Retrieve the startup information for various parts of the app, including feature flags. [More...](docs/startup.md) |


# Sandbox
All the above endpoints are accessible on sandbox with `/sandbox` prefix on each endpoint, e.g.
```
    GET /sandbox/startup
```

To trigger the sandbox endpoints locally, use the "X-MOBILE-USER-ID" header with the following value:
"DEMO-ACCOUNT"

To test different scenarios, add a header "SANDBOX-CONTROL" to specify the appropriate status code and return payload. 
See each linked file for details:

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/startup``` | GET | Acts as a stub for the related live endpoint. [More...](docs/sandbox/startup.md)  |


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
