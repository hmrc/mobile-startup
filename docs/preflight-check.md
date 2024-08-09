preflight-check
----
The response to this service includes a set of account identifiers for the user.

* **URL**

  `/mobile-startup/preflight-check`

* **Headers:**

  **Accept** -> `application/vnd.hmrc.1.0+json`

  **Authorization** -> `Bearer Token`

* **Method:**

  `GET`

* **URL Params**

  **Required:**

  `journeyId=[journeyId]`

  The journey ID is used for logging and diagnostic purposes.

* **Success Response:**

    * **Code:** 200 <br />
      **Content:**

```json
{
  "nino": "WX772755B",
  "utr": {
    "saUtr": "618567",
    "status": "activated"
  },
  "demoAccount": false,
  "routeToIV": false,
  "routeToTEN": false,
  "annualTaxSummaryLink": {
    "link": "/",
    "destination": "PAYE"
  }
}
```


