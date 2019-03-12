preflight-check
----
Validate the mobile application version and return account identifiers for the user. 

The response to this service includes an upgrade status flag and a set of account identifiers for the user. It will
also include a journey id. If the application does not provide a journey id as a url parameter then the service will
generate a new, unique, journey id that it can use for subsequent api calls.

The upgrade status is determined using the supplied POST data.
  
* **URL**

  `/mobile-startup/preflight-check`

* **Method:**
  
  `POST`

*  **URL Params**

   **Optional:**
 
   `journeyId=[journeyId]`
   
   The journey Id may be supplied for logging and diagnostic purposes and will be returned in the body of the response. If
   it is not supplied as a parameter then the service will generate a new, unique, value.
     
*  **JSON**

Current version information of the application. The "os" attribute can be either ios or android.

```json
{
    "os": "ios",
    "version" : "0.1.0"
}
```

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** 

```json
{
  "upgradeRequired": false,
  "account": {
    "nino": "WX772755B",
    "saUtr": "618567",
    "routeToIV": false,
    "journeyId": "f880d43b-bc44-4a68-b2e3-c0197963f01e"
  }
}
```


