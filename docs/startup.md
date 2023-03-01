Retrieve startup information
----

The response will include a flag to say if the tax credits window is open or closed, basic information
about the user's help-to-save-account (if they have one) and any global feature flags for the application.

* **URL**

  `/startup`
  
* **Headers:**  
  
    **Accept** -> `application/vnd.hmrc.1.0+json`
  
    **Authorization** -> `Bearer Token`
    
*  **URL Params**

   **Required:**
 
   `journeyId=[journeyId]`
   
   The journey ID is used for logging and diagnostic purposes.
    
* **Method:**

`GET`

  Example JSON response payload.
  
```json
{
  "taxCreditRenewals": {
    "submissionsState": "open"
  },
  "helpToSave": {
    "shuttering": {
      "shuttered": false,
      "title": "",
      "message": ""
    },
    "enabled": true,
    "balanceEnabled": true,
    "paidInThisMonthEnabled": true,
    "firstBonusEnabled": true,
    "savingRemindersEnabled": true,
    "infoUrl": "https://www.gov.uk/government/publications/help-to-save-what-it-is-and-who-its-for/the-help-to-save-scheme",
    "accessAccountUrl": "/",
    "user": {
      "state": "Enrolled"
    }
  },
  "feature": [
    {
      "name": "userPanelSignUp",
      "enabled": false
    },
    {
      "name": "ssoEnabled",
      "enabled": false
    }
  ],
  "urls": [
    {
      "name": "cbProofOfEntitlementUrl",
      "url": "/"
    },
    {
      "name": "cbProofOfEntitlementUrlCy",
      "url": "/"
    },
    {
      "name": "cbPaymentHistoryUrl",
      "url": "/"
    },
    {
      "name": "cbPaymentHistoryUrlCy",
      "url": "/"
    }
}
```
