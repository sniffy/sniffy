# Linux VM for the Codex CLI worker

This runbook provisions the unattended `codex-cli-v1` executor on a dedicated Linux virtual machine. It is the recommended
production topology for the repository-owned CLI worker.

Use a direct Linux VM, not Windows-in-a-VM with nested WSL. The Windows Codex app is not installed in this guest, no Windows drive
is mounted, and the Linux user's `~/.codex` is the only Codex state directory. This keeps the worker independent from app-native
threads, worktrees, configuration, authentication, updates, and model selection.

## Recommended guest

Use **Ubuntu Server 24.04 LTS x86_64** in a Hyper-V Generation 2 VM. It matches the CLI adapter's validation host, has mature
systemd and Docker support, and remains supported for the expected life of this worker. Prefer the current 24.04 point-release ISO
from [Ubuntu Server](https://ubuntu.com/download/server), not an unofficial appliance image.

For the Ryzen 9 9900X / 64 GB host, start with:

| Resource | Setting |
| --- | ---: |
| Virtual processors | 10 |
| Static memory | 24 GB |
| Dynamically expanding VHDX | 250 GB |
| Generation | 2, Secure Boot with Microsoft UEFI CA |
| Network | Hyper-V Default Switch |
| Guest account | dedicated local user `worker` |

Use 32 GB only for unusually large browser/Docker or multi-JDK verification. Keep one lifecycle worker active until the
single-worker recovery contract has been proven in normal operation.

Example elevated PowerShell on the Windows host:

```powershell
$vmName = "sniffy-codex-linux"
$vmRoot = "C:\work\vms\$vmName"
$isoPath = "C:\work\iso\ubuntu-24.04-live-server-amd64.iso"

New-Item -ItemType Directory -Force -Path $vmRoot
New-VM `
  -Name $vmName `
  -Generation 2 `
  -MemoryStartupBytes 24GB `
  -NewVHDPath "$vmRoot\system.vhdx" `
  -NewVHDSizeBytes 250GB `
  -SwitchName "Default Switch"
Set-VMProcessor -VMName $vmName -Count 10
Set-VMMemory -VMName $vmName -DynamicMemoryEnabled $false -StartupBytes 24GB
Set-VMFirmware `
  -VMName $vmName `
  -EnableSecureBoot On `
  -SecureBootTemplate MicrosoftUEFICertificateAuthority
Set-VM -Name $vmName -AutomaticStartAction Start -AutomaticStopAction Save
$dvd = Add-VMDvdDrive -VMName $vmName -Path $isoPath -Passthru
Set-VMFirmware -VMName $vmName -FirstBootDevice $dvd
Start-VM $vmName
```

During Ubuntu installation, create `worker`, enable OpenSSH, use the whole virtual disk, and install no desktop. Keep the VHDX on a
BitLocker-protected host volume if the VM must boot unattended. Create a clean pre-credential checkpoint after OS updates; never
restore a credential-bearing checkpoint without rotating the GitHub and Codex credentials that it contains.

## Base operating system

Log in as `worker` and install the non-language prerequisites:

```bash
sudo apt-get update
sudo apt-get -y full-upgrade
sudo apt-get install -y \
  bash bubblewrap build-essential ca-certificates curl fd-find git git-lfs \
  jq openssh-server ripgrep rsync shellcheck tar unzip zip
sudo timedatectl set-timezone UTC
sudo systemctl enable --now ssh
sudo systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target
git lfs install
```

Install GitHub CLI using its current official Linux instructions, then verify `gh --version`. The package distributed for Ubuntu is
sufficient when it provides `gh api`, `gh issue`, `gh pr`, and `gh project item-list`. See the
[GitHub CLI quickstart](https://docs.github.com/en/github-cli/github-cli/quickstart).

Install Docker Engine from Docker's official apt repository, including `docker-buildx-plugin` and `docker-compose-plugin`; do not
install Docker Desktop in the guest. See [Install Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/). Then:

```bash
sudo usermod -aG docker worker
newgrp docker
docker run --rm hello-world
```

Docker-group membership is effectively root. It is acceptable only because this guest is dedicated, disposable, and contains no
host mounts or unrelated credentials.

## Repository toolchains

Use `mise` so Node, Maven, and the Java compatibility matrix are reproducible for both login shells and the systemd worker:

```bash
curl https://mise.run | sh
export PATH="$HOME/.local/bin:$PATH"

mise install node@24 maven@3.9 java@8 java@11 java@17 java@21 java@25
mise use --global node@24 maven@3.9 java@25

node --version
npm --version
mvn --version
for version in 8 11 17 21 25; do
  mise x "java@${version}" -- java -version
done
```

Node 24 is required by `sniffy-ui`. Java 25 is the normal development default; Java 8, 11, 17, and 21 remain installed for the
repository's compatibility and verification lanes.

## Standalone Codex CLI

Install the standalone Linux CLI as `worker`, following the
[official Codex CLI setup](https://developers.openai.com/codex/cli):

```bash
curl -fsSL https://chatgpt.com/codex/install.sh | sh
export PATH="$HOME/.local/bin:$PATH"
codex --version
codex doctor
```

Do not set `CODEX_HOME` to a Windows path and do not copy the Windows app's `.codex` directory. For this guest, leave
`CODEX_HOME` unset so state stays under `/home/worker/.codex`.

For a headless ChatGPT sign-in, enable device-code login in ChatGPT security settings and run:

```bash
unset CODEX_HOME
mkdir -p -m 700 "$HOME/.codex"
codex login --device-auth
codex login status
chmod 600 "$HOME/.codex/auth.json" 2>/dev/null || true
```

Device login and credential-storage behavior are documented in
[Codex authentication](https://developers.openai.com/codex/auth). `auth.json` contains refresh credentials; never commit it, put it
in a VM image, paste it into an issue, or mount it from the host. The CLI refreshes a used ChatGPT session in place. If policy later
requires API-key or enterprise access-token authentication, change only this guest's login method.

The executor uses `codex exec --json`, explicit model/reasoning flags, and `danger-full-access` inside the disposable guest. OpenAI
documents `danger-full-access` only for controlled environments; the VM is the outer isolation boundary. See
[non-interactive mode](https://developers.openai.com/codex/non-interactive-mode) and
[agent approvals and security](https://developers.openai.com/codex/agent-approvals-security).

## GitHub identity and checkout

Use the dedicated `bedrin-codex-local` repository identity. The token needs only:

- repository metadata read;
- contents read/write for `sniffy/sniffy`;
- issues and pull requests read/write;
- organization Project 2 read/write;
- workflow write only when this executor is deliberately allowed to edit workflow files.

Authenticate without putting the token in a process argument or remote URL:

```bash
read -rsp "GitHub token: " github_token && echo
printf '%s\n' "$github_token" | \
  gh auth login --hostname github.com --git-protocol https --with-token
unset github_token
gh auth setup-git --hostname github.com
gh auth status --hostname github.com
```

Configure the distinct commit identity and clone only the worker repository:

```bash
git config --global user.name "bedrin-codex-local"
git config --global user.email "bedrin-codex-local@users.noreply.github.com"
mkdir -p "$HOME/src/sniffy"
git clone --branch develop https://github.com/sniffy/sniffy.git "$HOME/src/sniffy/sniffy"
cd "$HOME/src/sniffy/sniffy"
git status --short --branch
```

Do not mount `C:`, OneDrive, employer directories, SSH-agent sockets, password-manager sockets, or unrelated repositories in this
guest. Access it from the host over SSH when needed:

```powershell
Get-VMNetworkAdapter -VMName "sniffy-codex-linux" |
  Select-Object -ExpandProperty IPAddresses
```

## Bootstrap Sniffy dependencies

Warm the normal development toolchains before enabling dispatch:

```bash
cd "$HOME/src/sniffy/sniffy"
mvn --version

cd sniffy-ui
npm ci
npx playwright install --with-deps chromium firefox webkit
cd ..
```

Do not run `.codex/cloud/setup.sh` on this VM; it assumes Codex Cloud-provided runtimes and proxy behavior. Keep Maven, npm,
Playwright, Docker, and JDK caches inside the guest so every generation worktree can reuse them.

## Configure the systemd executor

Install the repository-owned units:

```bash
cd "$HOME/src/sniffy/sniffy"
mkdir -p "$HOME/.config/systemd/user" "$HOME/.config"
install -m 0644 .codex/local/systemd/*.service .codex/local/systemd/*.timer \
  "$HOME/.config/systemd/user/"
```

Resolve the JDK homes once and write the service environment. These values contain no secrets:

```bash
jdk8="$(mise where java@8)"
jdk11="$(mise where java@11)"
jdk17="$(mise where java@17)"
jdk21="$(mise where java@21)"
jdk25="$(mise where java@25)"

install -m 600 /dev/stdin "$HOME/.config/sniffy-local-executor.env" <<EOF
AI_DELIVERY_REPOSITORY=sniffy/sniffy
AI_DELIVERY_BASE_BRANCH=develop
AI_DELIVERY_STATE_ROOT=/home/worker/.local/state/sniffy-local-executor
AI_DELIVERY_WORKTREE_ROOT=/home/worker/.local/state/sniffy-local-executor/worktrees
AI_DELIVERY_CODEX_DEFAULT_PROFILE=balanced
AI_DELIVERY_CODEX_ECONOMY_MODEL=gpt-5.6-luna
AI_DELIVERY_CODEX_ECONOMY_REASONING=low
AI_DELIVERY_CODEX_BALANCED_MODEL=gpt-5.6-terra
AI_DELIVERY_CODEX_BALANCED_REASONING=medium
AI_DELIVERY_CODEX_FRONTIER_MODEL=gpt-5.6-sol
AI_DELIVERY_CODEX_FRONTIER_REASONING=high
SNIFFY_JDK8_HOME=$jdk8
SNIFFY_JDK11_HOME=$jdk11
SNIFFY_JDK17_HOME=$jdk17
SNIFFY_JDK21_HOME=$jdk21
SNIFFY_JDK25_HOME=$jdk25
JAVA_HOME=$jdk25
PATH=/home/worker/.local/bin:/home/worker/.local/share/mise/shims:/usr/local/bin:/usr/bin:/bin
EOF
```

Do not add `CODEX_MODEL` or `CODEX_REASONING_EFFORT`. The dispatcher resolves a named profile for each generation and the launcher
reads the immutable model/reasoning pair from that generation's manifest.

Allow the user systemd manager to run after logout, but keep the timer disabled until smoke tests pass:

```bash
sudo loginctl enable-linger worker
systemctl --user daemon-reload
systemctl --user disable --now sniffy-local-dispatch.timer 2>/dev/null || true
systemd-analyze verify \
  "$HOME/.config/systemd/user/sniffy-local-dispatch.service" \
  "$HOME/.config/systemd/user/sniffy-local-dispatch.timer" \
  "$HOME/.config/systemd/user/sniffy-local-worker@.service"
```

## Smoke test and enable

Run non-mutating validation first:

```bash
cd "$HOME/src/sniffy/sniffy"
bash -n .codex/local/cli-*.sh
shellcheck -x -S warning .codex/local/cli-*.sh
node --test .codex/local/cli-worker-state.test.js
bash .codex/local/project-queue-snapshot.sh | jq '{generatedAt, ownedInProgress, readyCandidates}'
codex login status
gh auth status --hostname github.com
```

Then prove the runtime contract in this order:

1. With no eligible Local Codex candidate, start `sniffy-local-dispatch.service` once and confirm `NO_CHANGE`, no generation,
   worktree, or Codex thread.
2. Route one disposable issue and set `Codex worker profile: economy`; prove one claim, generation, worktree, thread, exact branch,
   pull request, and lifecycle handoff.
3. Repeat with `balanced`, then use `frontier` only for a deliberately recorded difficult task.
4. Stop a test worker after acknowledgement and prove the next tick recovers the exact manifest without duplicate branch or PR.
5. Confirm the Windows Codex app has no project, config, credential, or worktree relationship to this VM.

Inspect evidence with:

```bash
systemctl --user status sniffy-local-dispatch.service
journalctl --user -u sniffy-local-dispatch.service -n 200 --no-pager
journalctl --user -u 'sniffy-local-worker@*.service' -n 200 --no-pager
find "$HOME/.local/state/sniffy-local-executor/generations" -maxdepth 2 -type f -print
```

After all tests pass:

```bash
systemctl --user enable --now sniffy-local-dispatch.timer
systemctl --user list-timers sniffy-local-dispatch.timer
```

The checked-in timer runs every 15 minutes with no randomized delay. Pause it immediately after unexpected claims, duplicate
publication, authentication failure, repeated recovery conflict, or Project/manifest mismatch.

## Maintenance and rebuild

- Update Ubuntu, Docker, GitHub CLI, Codex CLI, mise toolchains, and Playwright browsers in a deliberate maintenance window.
- Re-run Bash, ShellCheck, Node, systemd, empty-queue, and one-candidate smoke tests after material CLI or systemd changes.
- Monitor VM disk, Docker storage, Maven/npm caches, generation manifests, retained dirty worktrees, and the user journal.
- Keep GitHub branch protection authoritative: no force-push, deletion, merge, or protection bypass for the worker identity.
- Revoke both identities and rebuild the disposable VM after suspected compromise; do not repair an unknown credential-bearing
  guest in place.

