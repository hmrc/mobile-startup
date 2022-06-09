Preflight check
----
Acts as a stub to the /preflight-check endpoint.

To trigger the sandbox endpoints locally, either access the /sandbox endpoint directly or supply the use the
"X-MOBILE-USER-ID" header with one of the following values: 208606423740 or 167927702220

* **URL**

  `/sandbox/preflight-check`

* **Method:**

  `GET`

* **URL Params**

  **Required:**
  `journeyId=[String]`

  a string which is included for journey tracking purposes but has no functional impact


* **Success Responses:**

  To test different scenarios, add a header "SANDBOX-CONTROL" with one of the following values:

  | *Value* | *Description*
    |---------|---------------|
  | Not set or any value not specified below | Happy path, returns response with all details |
  | "ROUTE-TO-IV" | Returns response telling user to go through IV |
  | "ROUTE-TO-TEN" | Returns response telling user to go to TEN |
  | "ERROR-401" | Triggers a 401 Unauthorized response |
  | "ERROR-403" | Triggers a 403 Forbidden response |
  | "ERROR-500" | Triggers a 500 Internal Server Error response |
