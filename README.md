# mobile-startup

Provide the mobile apps with information they need at the time that they are launched.

(This service replaced functionality that used to be provided by `native-apps-api-orchestration`)

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

To trigger the sandbox endpoints locally, use the "X-MOBILE-USER-ID" header with one of the following values:
208606423740 or 167927702220

To test different scenarios, add a header "SANDBOX-CONTROL" to specify the appropriate status code and return payload. 
See each linked file for details:

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/preflight-check``` | GET | Acts as a stub for the related live endpoint. [More...](docs/sandbox/preflight-check.md)  |
| ```/startup``` | GET | Acts as a stub for the related live endpoint. [More...](docs/sandbox/startup.md)  |


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
