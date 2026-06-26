# How to Push This Repo to GitHub

The local repo is already initialised and has a single commit on the
`main` branch (`8d00e4b Initial commit of PVP Club Easy Invite v1.2.0`).
GitHub CLI (`gh`) is not installed on this machine, so the steps below
use **HTTPS + a Personal Access Token (PAT)** — the recommended way in
2026. SSH works too; both options are at the bottom.

## Option A — HTTPS (recommended)

### 1. Create the repo on GitHub

1. Go to <https://github.com/new> (you're signed in as `Pig4484` already).
2. **Owner**: `Pig4484`
3. **Repository name**: `pvpclub-easyinv`
4. **Description**: `Click any player name in chat to auto-/p invite them. Built for mcpvp.club.`
5. **Public** (Modrinth requires a public source repo, or at least one that's accessible).
6. **Do NOT** tick "Add a README file", "Add .gitignore", or "Choose a license". The repo must be empty for the first push.
7. Click **Create repository**.

You'll land on a page that says "Quick setup — if you've done this kind of thing before" with a URL like:

```
https://github.com/Pig4484/pvpclub-easyinv.git
```

Copy that URL.

### 2. Create a Personal Access Token (one-time setup)

GitHub removed password authentication in 2021. You need a PAT:

1. Go to <https://github.com/settings/tokens> → **Generate new token** → **Fine-grained tokens** (or "classic" if you prefer simpler).
2. **Name**: `pvpclub-easyinv-local-push`
3. **Expiration**: 90 days (or whatever you like)
4. **Scopes**: only `repo` (full control of private repositories) — that's all we need for a public repo push.
5. Click **Generate token**.
6. **Copy the token immediately** — GitHub will never show it again.

### 3. Push from PowerShell

Open PowerShell in the project directory and run:

```powershell
cd "C:\Users\user\Desktop\program\pvpclub_easyinv"

# Use the URL you copied in step 1
git remote add origin https://github.com/Pig4484/pvpclub-easyinv.git

# Verify it landed
git remote -v

# Push (will prompt for credentials)
git push -u origin main
```

When Git asks for credentials:
- **Username**: `Pig4484`
- **Password**: paste the **token** (not your GitHub password)

Git will print something like:

```
Enumerating objects: 28, done.
Counting objects: 100% (28/28), done.
...
To https://github.com/Pig4484/pvpclub-easyinv.git
 * [new branch]      main -> main
Branch 'main' set up to track remote branch 'main' from 'origin'.
```

Refresh <https://github.com/Pig4484/pvpclub-easyinv> and you'll see all 22 files.

### 4. Save the credentials so future pushes don't ask (optional)

Git on Windows can store the token via the **Git Credential Manager**:

```powershell
git config --global credential.helper manager
```

Next time you push, Windows will prompt you once and remember the token.

---

## Option B — SSH

If you already have an SSH key on GitHub, this is the cleanest path:

```powershell
cd "C:\Users\user\Desktop\program\pvpclub-easyinv"

git remote add origin git@github.com:Pig4484/pvpclub-easyinv.git
git push -u origin main
```

If you don't have an SSH key yet:
1. `ssh-keygen -t ed25519 -C "pig4484.yt@gmail.com"` (press Enter at every prompt)
2. `cat ~/.ssh/id_ed25519.pub` → copy the output
3. Go to <https://github.com/settings/keys> → **New SSH key** → paste → save

---

## After the First Push

### Tell Modrinth where the source lives

When you publish on Modrinth, in the **Source** section, paste:

```
https://github.com/Pig4484/pvpclub-easyinv
```

Modrinth will auto-detect the license (MIT) and add a "Source" link on
your mod page.

### Add a one-line description and topics on the GitHub repo

After the first push, on the GitHub repo page, click the gear icon next
to **About** and add:

- **Description**: `Click any player name in chat to auto-/p invite them. Built for mcpvp.club.`
- **Website**: `https://modrinth.com/mod/pvpclub-easyinv` (paste after you publish there)
- **Topics**: `minecraft`, `fabric`, `mod`, `chat`, `pvp`

### Future pushes

```powershell
cd "C:\Users\user\Desktop\program\pvpclub-easyinv"

# Make your changes, then:
git add -A
git commit -m "Describe what you changed"
git push
```

---

## Troubleshooting

### `fatal: remote origin already exists`

You ran the `git remote add` line twice. Either:

```powershell
git remote set-url origin https://github.com/Pig4484/pvpclub-easyinv.git
```

or

```powershell
git remote remove origin
git remote add origin https://github.com/Pig4484/pvpclub-easyinv.git
```

### `support for password authentication was removed`

You're using your GitHub password instead of a token. Generate a PAT
(step 2 above) and use that as the password.

### `Repository not found`

Either the URL is wrong, or the repo doesn't exist yet (you skipped step 1).
Double-check at <https://github.com/Pig4484?tab=repositories>.

### `Permission denied (publickey)`

You're trying SSH but don't have an SSH key, or GitHub doesn't have your
public key. Either switch to Option A, or follow the SSH key setup
under "Option B".

### `Updates were rejected because the remote contains work that you do not have locally`

The remote already has commits you don't have (e.g. you ticked "Add
README" on the GitHub create form). Fix one of two ways:

```powershell
# Option 1: force-push your local main over the remote
git push -u origin main --force

# Option 2: pull first, then push
git pull origin main --rebase
git push -u origin main
```

I recommend Option 1 since this is the very first push and your local
repo is the canonical one.

---

## Done?

Once `git push` returns successfully, your code is public at
<https://github.com/Pig4484/pvpclub-easyinv>. Drop that URL into
Modrinth's source field when you publish.
