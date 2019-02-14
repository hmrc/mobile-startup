
# ** Under Development **

# mobile-startup

Provide the mobile apps with information they need at the time that they are launched.

(This service replaced functionality that used to be provided by `native-apps-api-orchestration`)

The following services are exposed:

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/version-check``` | POST | Check if the user needs to update to a new version of the app. [More...](docs/version-check.md) |
| ```/startup``` | GET | Retrieve the startup information for various parts of the app, including feature flags. [More...](docs/startup.md) |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
