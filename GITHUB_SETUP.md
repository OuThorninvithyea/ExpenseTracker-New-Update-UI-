# How to Push Expense Tracker App to GitHub

This guide will walk you through pushing your Expense Tracker Android app to GitHub.

## Prerequisites
- GitHub account (create one at https://github.com if you don't have one)
- Git installed on your computer (usually comes with Android Studio)

## Step-by-Step Instructions

### Step 1: Create a GitHub Repository

1. Go to https://github.com and sign in
2. Click the **"+"** icon in the top right corner
3. Select **"New repository"**
4. Fill in the details:
   - **Repository name**: `expense-tracker-android` (or any name you prefer)
   - **Description**: "Android Expense Tracker App built with Java"
   - **Visibility**: Choose **Public** (free) or **Private** (requires GitHub Pro)
   - **DO NOT** check "Initialize this repository with a README" (we already have files)
5. Click **"Create repository"**

### Step 2: Initialize Git in Your Project

Open PowerShell or Command Prompt in your project directory and run:

```powershell
# Navigate to your project directory
cd "d:\Limkokwing\Android Studio\Expense Tracker app Java\Expense Tracker app Java"

# Initialize git repository
git init

# Add all files to staging
git add .

# Create initial commit
git commit -m "Initial commit: Expense Tracker Android App with login/signup functionality"
```

### Step 3: Connect to GitHub Repository

After creating the repository on GitHub, you'll see a page with setup instructions. Copy the repository URL (it looks like: `https://github.com/yourusername/expense-tracker-android.git`)

Then run:

```powershell
# Add GitHub repository as remote origin
git remote add origin https://github.com/yourusername/expense-tracker-android.git

# Rename default branch to main (if needed)
git branch -M main

# Push to GitHub
git push -u origin main
```

### Step 4: Verify Upload

1. Go to your GitHub repository page
2. You should see all your project files
3. Check that important files are there:
   - `app/src/main/java/` - All your Java source files
   - `app/src/main/res/` - Layout and resource files
   - `build.gradle.kts` - Build configuration

## Complete Command Sequence

Here's the complete sequence of commands to run:

```powershell
# Navigate to project directory
cd "d:\Limkokwing\Android Studio\Expense Tracker app Java\Expense Tracker app Java"

# Initialize git
git init

# Add all files
git add .

# Commit files
git commit -m "Initial commit: Expense Tracker Android App with login/signup functionality"

# Add remote repository (replace with your actual GitHub URL)
git remote add origin https://github.com/yourusername/your-repo-name.git

# Push to GitHub
git branch -M main
git push -u origin main
```

## If You Get Authentication Errors

If you get authentication errors when pushing:

### Option 1: Use GitHub Personal Access Token
1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token with `repo` permissions
3. Use token as password when prompted

### Option 2: Use GitHub Desktop
1. Download GitHub Desktop: https://desktop.github.com/
2. Sign in with your GitHub account
3. Add your local repository
4. Commit and push through the GUI

### Option 3: Use SSH Key
1. Generate SSH key: `ssh-keygen -t ed25519 -C "your_email@example.com"`
2. Add SSH key to GitHub: Settings → SSH and GPG keys → New SSH key
3. Use SSH URL instead: `git@github.com:yourusername/repo-name.git`

## Future Updates

After making changes to your code, push updates with:

```powershell
# Stage changes
git add .

# Commit changes
git commit -m "Description of your changes"

# Push to GitHub
git push
```

## Important Notes

- The `.gitignore` file is already configured to exclude:
  - Build files (`/build`)
  - IDE files (`.idea/`)
  - Local properties (`local.properties`)
  - Gradle cache (`.gradle`)

- **Never commit sensitive data** like:
  - API keys
  - Passwords
  - Keystore files
  - `local.properties` (already in .gitignore)

## Troubleshooting

### "Repository not found" error
- Check that the repository URL is correct
- Verify you have access to the repository
- Make sure you're authenticated

### "Permission denied" error
- Check your GitHub credentials
- Use Personal Access Token instead of password
- Verify SSH key is set up correctly

### "Large files" error
- Remove large files from commit history
- Use Git LFS for large files if needed
- Check `.gitignore` is excluding build artifacts

## Need Help?

- GitHub Docs: https://docs.github.com/en/get-started
- Git Documentation: https://git-scm.com/doc
- GitHub Support: https://support.github.com/
