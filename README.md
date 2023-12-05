**NB: archived until there's a consensus on the definition of 'expired' in PROD.**

# Delete expired Formstack data

This repo provides lambdas which are used for (respectively):
1. deleting expired Formstack forms;
2. deleting expired Formstack form submissions

This is to ensure our use of Formstack is GDPR compliant.

## Local development

### Setup

Start Docker if not running, then execute:
```
source ./scripts/setup
```

### Invoking a lambda locally

Start Docker if not running, then to get started execute:
```
./scripts/invoke_lambda -h
```

## FAQs

- _Why are there two Formstack accounts?_

  This was to mitigate against the (historical) limit of 25 users per Formstack account.


- _If new credentials are required to make authenticated Formstack API calls, who can these be obtained from?_

  All API calls require an access token; this can be provided by IT Service Desk. GET requests for submissions also 
  require the password that is used to encrypt submissions i.e. the encryption password; this can be provided by Central
  Production.


- _There are other Formstack lambdas in the [identity-processes](https://github.com/guardian/identity-processes) repository. 
Do these make the lambdas in this repo redundant?_

  No, the Formstack lambdas in the identity-processes repo have separate concerns.
    - `formstack-consents`: lambda which updates identity with consents that were made via Formstack forms
    - `formstack-baton-requests`: responsible for interacting with Formstack to fulfill right-to-erasure and subject-access-requests


- _Why is this process owned by EdTools?_

  If no 3rd-party tools were available, then this functionality would be provided by an in-house tool developed and maintained by EdTools.


- _Why does this sit in the Composer account?_

  It is related to content creation. 
