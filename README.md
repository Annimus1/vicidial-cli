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
| `campaign INFO`, `campaign PAUSE`, `campaign ACTIVE` | BASE_URL, API_USER, API_PASSWORD |
| `campaign CREATE` | the three above + SERVER_URL, plus SERVER_IP and TEMPLATE_ID when `-n` is used |
| `createCreds` | BASE_URL, API_USER, API_PASSWORD, SERVER_IP, TEMPLATE_ID |
| `updateCred` | BASE_URL, API_USER, API_PASSWORD, plus SERVER_IP when a password is given |
| `did list`, `did verify`, `did remove` | BASE_URL, API_USER, API_PASSWORD, SERVER_URL |
| `leadDetails`, `duplicateInList` | BASE_URL, API_USER, API_PASSWORD |

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

- ### createCreds — create user + phone credentials.
  - Usage: `vicidial-cli createCreds <ID> <password> <userGroupId> [-n|--name "<displayName>"]`
  - Example: `vicidial-cli createCreds agent001 S3cr3t UG_DEFAULT -n "Agent One"`

- ### duplicateInList — duplicate a lead into a list.
  - Usage: `vicidial-cli duplicateInList <leadId> <listId> [-c|--comments "<notes>"] [-e|--email <email>]`
  - Example: `vicidial-cli duplicateInList 12345 10 -c "Transfer to new list" -e new@example.com`

- ### getAllCampaigns — list campaigns.
  - Usage: `vicidial-cli getAllCampaigns`

- ### leadDetails — fetch full lead info.
  - Usage (CLI varies by implementation): `vicidial-cli leadDetails --leadId <ID>`

- ### updateCred — update user and phone credentials.
  - Usage: `vicidial-cli updateCred <ID> [-n|--name "<displayName>"] [-p|--password <newPassword>]`
  - Example: `vicidial-cli updateCred agent001 -n "John Doe" -p N3wP@ss`

- ### did — work with the DIDs stored on the instance.
  - Subcommands:
    - `did list` — show the DIDs on the instance, with `--active`, `--inactive`, `--group`, `--carrier` filters, or DID numbers as positional arguments
    - `did verify <number>` — check that a DID exists and show its group, route, carrier and description; exits `1` when it is not on the instance
    - `did remove` — remove DIDs, selected with exactly one of `--did`, `--file` or `--group`; supports `--dry-run` and `--force`
    - `did add` — not implemented yet
  - Subcommand names are case-insensitive, so `did LIST` also works.
  - DID format validation: must start with `1` and be 11 digits (e.g. `15551234567`).
  - A bulk removal asks for confirmation; `--force` skips it, `--dry-run` previews without changing anything.
  - Examples:
    - `vicidial-cli did list`
    - `vicidial-cli did verify 15551234567`
    - `vicidial-cli did remove --did 15551234567`
    - `vicidial-cli did remove --file /path/to/dids.txt`
    - `vicidial-cli did remove --group SALES_TEAM --dry-run`

## Examples

- List campaigns:
  `vicidial-cli getAllCampaigns`

- Lead details:
  `vicidial-cli leadDetails --leadId 12345`

- Duplicate lead into list:
  `vicidial-cli duplicateInList 12345 678 -c "copied" -e "x@example.com"`

- Create credentials:
  `vicidial-cli createCreds agent001 secret_pass SALES -n "Agent Name"`

- Update credential:
  `vicidial-cli updateCred agent001 -n "New Name" -p newpass`

## Notes & Troubleshooting

- Ensure BASE_URL points to the correct API endpoint or admin base.
- If using admin HTML pages (DIDs) the app uses HTTP Basic auth with API_USER/API_PASSWORD.
- Timeouts are configured; network failures surface as errors.
- If a command throws credential/config errors, confirm `.env` or environment variables are set.

---

### Contributions and issues welcome.