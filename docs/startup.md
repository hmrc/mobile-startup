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
  "taxCreditRenewals": {
    "submissionsState": "open"
  },
  "messages": {
    "home": [],
    "paye": [
      {
        "id": "paye-message-1",
        "type": "Info",
        "headlineContent": {
          "title": "Title2 - Has active date",
          "body": "Content2"
        },
        "activeWindow": {
          "startTime": "2020-03-01T20:06:12.726",
          "endTime": "2030-05-24T20:06:12.726"
        }
      }
    ],
    "tc": [
      {
        "id": "tc-message-1",
        "type": "Info",
        "headlineContent": {
          "title": "Title2 - Has active date",
          "body": "Content2"
        },
        "activeWindow": {
          "startTime": "2020-03-01T20:06:12.726",
          "endTime": "2030-05-24T20:06:12.726"
        }
      },
      {
        "id": "tc-message-2",
        "type": "Info",
        "headlineContent": {
          "title": "Title2 - Has active date",
          "body": "Content2"
        },
        "activeWindow": {
          "startTime": "2020-03-01T20:06:12.726",
          "endTime": "2030-05-24T20:06:12.726"
        }
      },
      {
        "id": "tc-message-2",
        "type": "Urgent",
        "headlineContent": {
          "title": "Ftnae is no more",
          "body": "Content2"
        },
        "activeWindow": {
          "startTime": "2020-02-01T20:06:12.726",
          "endTime": "2030-05-24T20:06:12.726"
        }
      }
    ],
    "hts": [
      {
        "id": "hts-message-1",
        "type": "Warning",
        "headlineContent": {
          "title": "Title3",
          "body": "Content3",
          "links": [
            {
              "url": "<REPLACE_URL>",
              "urls": [
                "URL3",
                "URL2"
              ],
              "urlType": "Normal",
              "type": "Secondary",
              "message": "Click me",
              "androidCampaignQueryString": "androidCampaignQueryString",
              "iosCampaignQueryString": "iosCampaignQueryString"
            }
          ]
        }
      },
      {
        "id": "hts-message-2",
        "type": "Urgent",
        "headlineContent": {
          "title": "Title4",
          "body": "Content4",
          "links": [
            {
              "url": "URL4",
              "urls": [
                "URL4"
              ],
              "urlType": "Normal",
              "type": "Secondary",
              "message": "Click me",
              "androidCampaignQueryString": "androidCampaignQueryString",
              "iosCampaignQueryString": "iosCampaignQueryString"
            }
          ]
        },
        "icon": "Info"
      }
    ],
    "tcp": [
      {
        "id": "tcp-message-1",
        "type": "Notice",
        "headlineContent": {
          "title": "Title3",
          "links": [
            {
              "url": "/More Info",
              "urls": [
                "/More Info"
              ],
              "urlType": "NewScreen",
              "type": "Secondary",
              "message": "More Info"
            }
          ]
        },
        "newScreenContent": {
          "title": "newScreenTitle",
          "screenTitle": "newScreen screenTitle",
          "body": "Content4",
          "links": [
            {
              "url": "URL2",
              "urls": [
                "URL2"
              ],
              "urlType": "InApp",
              "type": "Primary",
              "message": "Click me first"
            },
            {
              "url": "URL3",
              "urls": [
                "URL3"
              ],
              "urlType": "Normal",
              "type": "Secondary",
              "message": "Click me",
              "androidCampaignQueryString": "androidCampaignQueryString",
              "iosCampaignQueryString": "iosCampaignQueryString"
            }
          ]
        },
        "icon": "Warning"
      }
    ],
    "money": [],
    "communication": [],
    "details": [],
    "sa": [],
    "cb": [],
    "taxcalc": []
  },
  "user": {
    "name": "BELINDA MARY OAKES",
    "address": {
      "line1": "999 Big Street",
      "line2": "Worthing",
      "line3": "West Sussex",
      "postcode": "BN99 8IG"
    }
  },
  "childBenefit": {
    "shuttering": {
      "shuttered": false
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
    },
    {
      "name": "cbChangeBankAccountUrl",
      "url": "/"
    },
    {
      "name": "cbChangeBankAccountUrlCy",
      "url": "/"
    },
    {
      "name": "statePensionUrl",
      "url": "/"
    },
    {
      "name": "niSummaryUrl",
      "url": "/"
    },
    {
      "name": "niContributionsUrl",
      "url": "/"
    },
    {
      "name": "cbHomeUrl",
      "url": "/"
    },
    {
      "name": "cbHomeUrlCy",
      "url": "/"
    },
    {
      "name": "cbHowToClaimUrl",
      "url": "/"
    },
    {
      "name": "cbHowToClaimUrlCy",
      "url": "/"
    },
    {
      "name": "cbFullTimeEducationUrl",
      "url": "/"
    },
    {
      "name": "cbFullTimeEducationUrlCy",
      "url": "/"
    },
    {
      "name": "cbWhatChangesUrl",
      "url": "/"
    },
    {
      "name": "cbWhatChangesUrlCy",
      "url": "/"
    },
    {
      "name": "otherTaxesDigitalAssistantUrl",
      "url": "/"
    },
    {
      "name": "otherTaxesDigitalAssistantUrlCy",
      "url": "/"
    },
    {
      "name": "payeDigitalAssistantUrl",
      "url": "/"
    },
    {
      "name": "payeDigitalAssistantUrlCy",
      "url": "/"
    },
    {
      "name": "incomeTaxGeneralEnquiriesUrl",
      "url": "/"
    },
    {
      "name": "learnAboutCallChargesUrl",
      "url": "/"
    },
    {
      "name": "learnAboutCallChargesUrlCy",
      "url": "/"
    },
    {
      "name": "tcNationalInsuranceRatesLettersUrl",
      "url": "/"
    },
    {
      "name": "tcNationalInsuranceRatesLettersUrlCy",
      "url": "/"
    },
    {
      "name": "tcPersonalAllowanceUrl",
      "url": "/"
    },
    {
      "name": "tcPersonalAllowanceUrlCy",
      "url": "/"
    },
    {
      "name": "scottishIncomeTaxUrl",
      "url": "/"
    },
    {
      "name": "scottishIncomeTaxUrlCy",
      "url": "/"
    },
    {
      "name": "cbTaxChargeUrl",
      "url": "/"
    },
    {
      "name": "cbTaxChargeUrlCy",
      "url": "/"
    },
    {
      "name": "selfAssessmentHelpAppealingPenaltiesUrl",
      "url": "/"
    },
    {
      "name": "selfAssessmentHelpAppealingPenaltiesUrlCy",
      "url": "/"
    },
    {
      "name": "addMissingTaxableIncomeUrl",
      "url": "/"
    },
    {
      "name": "helpToSaveGeneralEnquiriesUrl",
      "url": "/"
    },
    {
      "name": "helpToSaveGeneralEnquiriesUrlCy",
      "url": "/"
    },
    {
      "name": "helpToSaveDigitalAssistantUrl",
      "url": "/"
    },
    {
      "name": "selfAssessmentGeneralEnquiriesUrl",
      "url": "/"
    },
    {
      "name": "selfAssessmentGeneralEnquiriesUrlCy",
      "url": "/"
    },
    {
      "name": "simpleAssessmentGeneralEnquiriesUrl",
      "url": "/"
    },
    {
      "name": "simpleAssessmentGeneralEnquiriesUrlCy",
      "url": "/"
    },
    {
      "name": "findRepaymentPlanUrl",
      "url": "/"
    },
    {
      "name": "findRepaymentPlanUrlCy",
      "url": "/"
    },
    {
      "name": "pensionAnnualAllowanceUrl",
      "url": "/"
    },
    {
      "name": "pensionAnnualAllowanceUrlCy",
      "url": "/"
    },
    {
      "name": "childBenefitDigitalAssistantUrl",
      "url": "/"
    },
    {
      "name": "childBenefitDigitalAssistantUrlCy",
      "url": "/"
    },
    {
      "name": "incomeTaxDigitalAssistantUrl",
      "url": "/"
    },
    {
      "name": "incomeTaxDigitalAssistantUrlCy",
      "url": "/"
    },
    {
      "name": "selfAssessmentDigitalAssistantUrl",
      "url": "/"
    },
    {
      "name": "selfAssessmentDigitalAssistantUrlCy",
      "url": "/"
    },
    {
      "name": "taxCreditsDigitalAssistantUrl",
      "url": "/"
    },
    {
      "name": "taxCreditsDigitalAssistantUrlCy",
      "url": "/"
    },
    {
      "name": "tcStateBenefitsUrl",
      "url": "/"
    },
    {
      "name": "tcStateBenefitsUrlCy",
      "url": "/"
    },
    {
      "name": "tcCompanyBenefitsUrl",
      "url": "/"
    },
    {
      "name": "tcCompanyBenefitsUrlCy",
      "url": "/"
    }
  ]
}
```
