# vicidial-cli

Lightweight CLI tooling to interact with Vicidial admin pages.

## Overview

Provides small commands to inspect and manage Vicidial data (DIDs, leads, users, phones). Commands are implemented with picocli and call the Vicidial API / admin pages.

## Requirements

- Java 17+
- Maven
- A Vicidial instance and valid credentials

## Build

mvn package

Run the produced JAR (adjust filename as produced by your build):

java -jar target/vicidial-cli.jar <command> [options]

Or run with Maven:

mvn -q exec:java -Dexec.mainClass="dev.pablo.cli.MainApplication" -Dexec.args="--help"

## Configuration

You can configure credentials via a `.env` file at the repository root or via environment variables.

Required variables:
- BASE_URL — base API URL (e.g. https://your-vicidial-server.com/agc/api.php)
- API_USER — API username (or admin user for admin page requests)
- API_PASSWORD — API password

Optional variables (used by some commands):
- SERVER_IP
- TEMPLATE_ID

Example `.env`:
```
BASE_URL=https://cloud.yourserviceva.net/vicidial/agc/api.php
API_USER=admin
API_PASSWORD=secret
SERVER_IP=10.0.0.1
TEMPLATE_ID=123
```

## Commands

All commands are subcommands of `vicidial-cli`. Run `vicidial-cli --help` for global help or `vicidial-cli <subcommand> --help` for details.

- createCreds — create user/phone credentials.
- duplicateInList — duplicate a lead into a list.
  - Options: lead id, list id, optional comments/email
- getAllCampaigns — list campaigns.
- leadDetails — fetch full lead info.
  - Usage: vicidial-cli leadDetails --leadId <ID>
- updateCred — update user/phone credentials.
- deleteDIDs — delete DIDs from Vicidial admin page.

## deleteDIDs usage

Modes:
- SINGLE: remove a single DID by exact number (--did)
- MULTIPLE: remove DIDs listed in a newline-separated file (-l / --list)
- GROUP: planned — remove by usergroup (not fully implemented)

DID format validation: must start with `1` and be 11 digits (e.g. `15551234567`).

Examples:
- Single DID:
  vicidial-cli deleteDIDs -m SINGLE --did 15551234567

- Multiple (file with one DID per line):
  vicidial-cli deleteDIDs -m MULTIPLE -l /path/to/dids.txt

- Group (planned):
  vicidial-cli deleteDIDs -m GROUP --group SALES_TEAM

## Examples

- List campaigns:
  vicidial-cli getAllCampaigns

- Lead details:
  vicidial-cli leadDetails --leadId 12345

- Duplicate lead into list:
  vicidial-cli duplicateInList --leadId 12345 --listId 678 --comments "copied" --email "x@example.com"

- Create credentials:
  vicidial-cli createCreds --id user123 --password passw0rd --name "User Name" --group SALES

- Update credential:
  vicidial-cli updateCred --id user123 --name "New Name" --password newpass

## Notes & Troubleshooting

- Ensure BASE_URL points to the correct API endpoint or admin base.
- If using admin HTML pages (DIDs) the app uses HTTP Basic auth with API_USER/API_PASSWORD.
- Timeouts are configured; network failures surface as errors.
- If a command throws credential/config errors, confirm .env or environment variables are set.

Contributions and issues welcome.