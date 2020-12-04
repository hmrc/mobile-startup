Startup
----
  Acts as a stub to the /startup endpoint.
  
  To trigger the sandbox endpoints locally, either access the /sandbox endpoint directly or supply the use the 
  "X-MOBILE-USER-ID" header with one of the following values: 208606423740 or 167927702220
  
* **URL**

  `/sandbox/startup`

* **Method:**

  `GET`

*  **URL Params**

   **Required:**
   `journeyId=[String]`

    a string which is included for journey tracking purposes but has no functional impact


* **Success Responses:**

  To test different scenarios, add a header "SANDBOX-CONTROL" with one of the following values:
  
  | *Value* | *Description* 
  |---------|---------------|
  | Not set or any value not specified below | Happy path, returns response with all details |
  | "RENEWALS-OPEN" | Happy path with renewals open |
  | "RENEWALS-VIEW-ONLY" | Happy path, can only view renewals and not submit |
  | "RENEWALS-CLOSED" | Happy path with renewals closed |
  | "HTS-ENROLLED" | Happy path where Help to Save user is enrolled |
  | "HTS-ELIGIBLE" | Happy path where Help to Save user is eligible but currently not enrolled |
  | "HTS-NOT-ENROLLED" | Happy path where Help to Save user is not enrolled and not eligible |
  | "ERROR-401" | Triggers a 401 Unauthorized response |
  | "ERROR-403" | Triggers a 403 Forbidden response |
  | "ERROR-500" | Triggers a 500 Internal Server Error response |
