# Delete expired Formstack data

This repo provides lambdas which are used for (respectively):
1. deleting expired Formstack forms;
2. deleting expired Formstack form submissions

This is to ensure our use of Formstack is GDPR compliant.

## Production

### Setup

The following parameters must be created in [parameter store](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html):
- `formstack/account-1/account-id`
- `formstack/account-1/access-token`
- `formstack/account-1/encryption-password`
- `formstack/account-2/account-id`
- `formstack/account-2/access-token`
- `formstack/account-2/encryption-password`

The rest of the resources can then be created using `template.yaml`.

### Alarms

An alarm is triggered if any `ERROR`s are logged.

## Local development

### Setup

```
source ./scripts/setup
```

### Invoking a lambda locally
```
./scripts/invoke_lambda -h
```

## FAQs

- _Why are there two Formstack accounts?_

  This was to mitigate against the (historical) limit of 25 users per Formstack account.


- _How do you get credentials to make authenticated Formstack API calls?_

  All API calls require an access token; this can be provided by TODO. GET requests for submissions also require 
the password that is used to encrypt submissions i.e. the encryption password; this can be provided by TODO


- _There are other Formstack lambdas in the [identity-processes](https://github.com/guardian/identity-processes) repository. 
Do these make the lambdas in this repo redundant?_

  No, the Formstack lambdas in the identity-processes repo have separate concerns.
    - `formstack-consents`: lambda which updates identity with consents that were made via Formstack forms
    - `formstack-baton-requests`: responsible for interacting with Formstack to fulfill right-to-erasure and subject-access-requests


- _Why is this process owned by EdTools?_

  If no 3rd-party tools were available, then this functionality would be provided by an in-house tool developed and maintained by EdTools.


- _Why does this sit in the Composer account?_

  It is related to content creation. 

- _For each entity type (forms, submissions), why is there a lambda per Formstack account instead of one lambda invoked
with credentials for the different Formstack accounts (respectively)?
  
  
