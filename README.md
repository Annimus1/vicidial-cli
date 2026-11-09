![Vicidial-cli](/img/vicidial-cli.png)

A lightweight, scriptable CLI for administering Vicidial instances. Designed for automation and bulk operations, vicidial-cli exposes targeted commands to manage users, phones, leads and DIDs via the Vicidial API and admin pages. Use it for quick inspections, one-off fixes, or to integrate into deployment and maintenance pipelines.

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

### Recommended installation (Unix)

For a stable, system-wide installation on Unix-like systems, install the built JAR under /opt and expose a small wrapper so the CLI is available in PATH:

```sh
# as a user with sudo privileges
sudo mkdir -p /opt/vicidial-cli
sudo cp target/vicidial-cli-{version}.jar /opt/vicidial-cli/vicidial-cli.jar
sudo chmod 755 /opt/vicidial-cli/vicidial-cli.jar

# create a simple wrapper executable (preferred over shell alias)
sudo tee /usr/local/bin/vicidial-cli > /dev/null <<'EOF'
#!/usr/bin/env bash
exec java -jar /opt/vicidial-cli/vicidial-cli.jar "$@"
EOF
sudo chmod +x /usr/local/bin/vicidial-cli
```

If you prefer a shell alias instead, add the following to your shell profile (e.g. ~/.bashrc or ~/.zshrc):

```sh
echo 'alias vicidial-cli="java -jar /opt/vicidial-cli/vicidial-cli.jar"' >> ~/.bashrc
# then reload: source ~/.bashrc
```

The wrapper script approach is recommended for reliability (preserves arguments and works for all users with PATH access).

## Configuration

vicidial-cli reads configuration from a .env file at the repository root or from environment variables. The following environment variables are recognized:

- BASE_URL — (required) Base API endpoint, e.g. https://your-vicidial-server.com/agc/api.php
- API_USER — (required) API username (or an admin user for admin-page requests)
- API_PASSWORD — (required) API password
- SERVER_IP — (required to create or update phones) SIP server IP used for phone operations
- TEMPLATE_ID — (required to create phones) Phone template id
- SERVER_URL — (required for admin pages) Web UI URL of the Vicidial admin, e.g. https://your-vicidial-server/vicidial/admin.php

The CLI validates configuration before doing any work and reports every missing
variable at once. Requirements per command:

| Command | Required variables |
|---------|--------------------|
| `campaign list`, `campaign info`, `campaign pause`, `campaign active` | BASE_URL, API_USER, API_PASSWORD |
| `campaign create` | the three above + SERVER_URL, plus SERVER_IP and TEMPLATE_ID when `--users` is used |
| `credential create` | BASE_URL, API_USER, API_PASSWORD, SERVER_IP, TEMPLATE_ID |
| `credential modify` | BASE_URL, API_USER, API_PASSWORD, plus SERVER_IP when a password is given |
| `credential list` | BASE_URL, API_USER, API_PASSWORD; SERVER_URL for the admin-page fallback |
| `credential active`, `credential pause` | BASE_URL, API_USER, API_PASSWORD |
| `did list`, `did verify`, `did remove` | BASE_URL, API_USER, API_PASSWORD, SERVER_URL |
| `list list` | BASE_URL, API_USER, API_PASSWORD, SERVER_URL |
| `list active`, `list pause`, `list move`, `list reset` | BASE_URL, API_USER, API_PASSWORD |
| `lead info`, `lead duplicate` | BASE_URL, API_USER, API_PASSWORD |

Examples

- .env file (placed at project root):
```
BASE_URL=https://cloud.yourserviceva.net/vicidial/agc/api.php
API_USER=admin
API_PASSWORD=secret
SERVER_IP=10.0.0.1
TEMPLATE_ID=123
SERVER_URL=https://your-vicidial-server/vicidial/admin.php
```

- Export environment variables in a shell session:
```sh
export BASE_URL="https://vicidial.example/api"
export API_USER="api_user"
export API_PASSWORD="supersecret"
export SERVER_IP="192.0.2.10"
export TEMPLATE_ID="123"
export SERVER_URL="https://vicidial.example/vicidial/admin.php"
```

When running the packaged JAR, ensure the environment variables are available to the process (persist via shell profile or export inline):

```sh
BASE_URL="https://vicidial.example/api" API_USER="api_user" API_PASSWORD="supersecret" \
  java -jar /opt/vicidial-cli/vicidial-cli.jar <command> [options]
```

## Commands

All commands are subcommands of `vicidial-cli`. Run `vicidial-cli --help` for global help or `vicidial-cli <subcommand> --help` for details.

- ### credential — work with the credentials (users and phones) on the instance.
  - Subcommands:
    - `credential create --id ID --password PASS --group GROUP [-n|--name "<displayName>"]` — create a credential with its user and phone
    - `credential list [--id ID]` — show every user, or one user; falls back to `user_details` / the admin Users page (`ADD=0A`) on builds without `users_list`
    - `credential modify --id ID [-n|--name "<displayName>"] [-p|--password <newPassword>]` — update a user's name and/or password
    - `credential active --id ID` — set a user to active
    - `credential pause --id ID` — set a user to inactive
  - `credential list` hides users with a user level above 8.
  - Subcommand names are case-insensitive, so `credential CREATE` also works.
  - Examples:
    - `vicidial-cli credential create --id agent001 --password S3cr3t --group UG_DEFAULT`
    - `vicidial-cli credential create --id agent001 --password S3cr3t --group UG_DEFAULT -n "Agent One"`
    - `vicidial-cli credential list`
    - `vicidial-cli credential list --id agent001`
    - `vicidial-cli credential modify --id agent001 -n "John Doe"`
    - `vicidial-cli credential active --id agent001`
    - `vicidial-cli credential pause --id agent001`

- ### campaign — work with the campaigns stored on the instance.
  - Subcommands:
    - `campaign list` — show every campaign
    - `campaign info [--id ID]` — show one campaign, or list every campaign when no id is given
    - `campaign create --id ID --description DESC [--users N] [--password PASS]` — create the campaign, its user/CID/inbound groups, list and optional agent users
    - `campaign pause --id ID` — set a campaign to inactive
    - `campaign active --id ID` — set a campaign to active
  - Subcommand names are case-insensitive, so `campaign LIST` also works.
  - Examples:
    - `vicidial-cli campaign list`
    - `vicidial-cli campaign info --id SALES`
    - `vicidial-cli campaign create --id SALES --description "Sales campaign"`
    - `vicidial-cli campaign create --id SALES --description "Sales campaign" --users 5 --password secret`
    - `vicidial-cli campaign pause --id SALES`
    - `vicidial-cli campaign active --id SALES`

- ### lead — work with the leads stored on the instance.
  - Subcommands:
    - `lead info --id ID` — show all information for one lead
    - `lead duplicate --lead-id ID --list-id ID [-c|--comments "<notes>"] [-e|--email <email>]` — duplicate a lead into a list
  - Subcommand names are case-insensitive, so `lead INFO` also works.
  - Examples:
    - `vicidial-cli lead info --id 1125`
    - `vicidial-cli lead duplicate --lead-id 12345 --list-id 10`
    - `vicidial-cli lead duplicate --lead-id 12345 --list-id 10 -c "Transfer to new list" -e new@example.com`

- ### did — work with the DIDs stored on the instance.
  - Subcommands:
    - `did list` — show the DIDs on the instance, with `--active`, `--inactive`, `--group`, `--carrier` filters, or DID numbers as positional arguments
    - `did verify <number>` — check that a DID exists and show its group, route, carrier and description; exits `1` when it is not on the instance
    - `did remove` — remove DIDs, selected with exactly one of `--did`, `--file` or `--group`; supports `--dry-run` and `--force`; add `--cid <group>` to also remove each outbound caller id from a CID group (`--areacode`, `--state` or `--all` control the entry areacode)
    - `did add` — add one DID (`--did`) or a file of DIDs (`--file`) to an inbound group (`--inbound`/`-i`) and a CID group (`--cid`/`-c`); the CID entry drops the leading `1`, uses `areacode=NONE` by default and supports `--areacode`, `--state` or `--all`. Add `--dry-run` to preview.
  - Subcommand names are case-insensitive, so `did LIST` also works.
  - DID format validation: must start with `1` and be 11 digits (e.g. `15551234567`).
  - A bulk removal asks for confirmation; `--force` skips it, `--dry-run` previews without changing anything.
  - Examples:
    - `vicidial-cli did list`
    - `vicidial-cli did verify 15551234567`
    - `vicidial-cli did remove --did 15551234567`
    - `vicidial-cli did remove --file /path/to/dids.txt`
    - `vicidial-cli did remove --group SALES_TEAM --dry-run`
    - `vicidial-cli did remove --did 15551234567 --cid SALES_CID`
    - `vicidial-cli did add --did 11231234321 --inbound SALES --cid SALESCID`
    - `vicidial-cli did add --file /path/to/dids.txt --inbound SALES --cid SALESCID --state FL`

- ### list — work with the lists stored on the instance.
  - Subcommands:
    - `list list` — show every list, with `--activeOnly`, `--inactiveOnly` and `--campaign` filters
    - `list active --id ID` — set a list to active
    - `list pause --id ID` — set a list to inactive
    - `list move --campaignId CAMPAIGN --id LIST_ID` — move a list to a different campaign
    - `list reset --id LIST_ID` — reset the called-since-last-reset flag for every lead in the list
  - Subcommand names are case-insensitive, so `list LIST` also works.
  - The `active`, `pause`, `move` and `reset` commands call the Non-Agent API
    `update_list`; the API user needs user level 8 or higher with "Modify Lists"
    enabled.
  - Examples:
    - `vicidial-cli list list`
    - `vicidial-cli list list --activeOnly`
    - `vicidial-cli list list --campaign SALES`
    - `vicidial-cli list active --id 6004`
    - `vicidial-cli list pause --id 6004`
    - `vicidial-cli list move --campaignId SALES --id 6004`
    - `vicidial-cli list reset --id 6004`

## Examples

- List campaigns:
  `vicidial-cli campaign list`

- Lead details:
  `vicidial-cli lead info --id 12345`

- Duplicate lead into list:
  `vicidial-cli lead duplicate --lead-id 12345 --list-id 678 -c "copied" -e "x@example.com"`

- Create credentials:
  `vicidial-cli credential create --id agent001 --password secret_pass --group SALES -n "Agent Name"`

- Update credential:
  `vicidial-cli credential modify --id agent001 -n "New Name" -p newpass`

## Notes & Troubleshooting

- Ensure BASE_URL points to the correct API endpoint or admin base.
- If using admin HTML pages (DIDs) the app uses HTTP Basic auth with API_USER/API_PASSWORD.
- Timeouts are configured; network failures surface as errors.
- If a command throws credential/config errors, confirm `.env` or environment variables are set.

---

### Contributions and issues welcome.