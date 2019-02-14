
# ** Under Development **

# mobile-startup

Provide the mobile apps with information they need at the time that they are launched.

(This service replaced functionality that used to be provided by `native-apps-api-orchestration`)

The following services are exposed.

Please note it is mandatory to supply an Accept HTTP header to all below services with the value ```application/vnd.hmrc.1.0+json```. 

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/startup``` | GET | Retrieve the startup information for various parts of the app, including feature flags. [More...](docs/startup.md) |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
