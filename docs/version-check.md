version-check
----
Validate the mobile application version. The response to this service includes an upgrade status flag and, optionally, a Journey Id.

The upgrade status is determined using the supplied POST data.
  
* **URL**

  `/mobile-startup/version-check`

* **Method:**
  
  `POST`

*  **URL Params**

   **Optional:**
 
   `journeyId=[journeyId]`
   
   The journey Id may be supplied for logging and diagnostic purposes.
     
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
  "upgradeRequired": false
}
```


