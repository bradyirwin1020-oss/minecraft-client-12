# Build Aegis JAR on GitHub Actions

This project includes `.github/workflows/build.yml`. GitHub Actions will build the Fabric mod using JDK 25 and Gradle 9.8.2. It uploads `build/libs/aegis-client-2.0.0.jar` as a downloadable workflow artifact.

## Recommended: push the project using GitHub CLI

1. Create/sign in to a GitHub account in your browser.
2. In a terminal on Bazzite, install Git and GitHub CLI:

   ```bash
   sudo dnf5 install -y git gh
   ```

3. Authenticate through GitHub's browser/device flow:

   ```bash
   gh auth login --hostname github.com --git-protocol https --web
   ```

   Follow the displayed code and browser instructions. Do not paste your password or access token into chat or into project files.

4. Enter the project directory:

   ```bash
   cd ~/Downloads/nexus_build
   ```

5. Set a Git commit identity (use your preferred display name and email):

   ```bash
   git config user.name "YOUR GITHUB USERNAME"
   git config user.email "YOUR GITHUB EMAIL"
   ```

6. Initialize and commit the project:

   ```bash
   git init -b main
   git add .
   git commit -m "Add Aegis Client and GitHub Actions build"
   ```

7. Create a private GitHub repository and push the code:

   ```bash
   gh repo create aegis-client --private --source=. --remote=origin --push
   ```

   If you prefer a public repository, replace `--private` with `--public`. A private repository is the safer default if you do not want the source publicly visible.

## Run the build

The workflow starts automatically after the first push and every later push to `main` or `master`. You can also run it manually:

1. Open `https://github.com/YOUR-USERNAME/aegis-client/actions`.
2. Select **Build Aegis Client JAR**.
3. Select **Run workflow**, choose `main`, and confirm.
4. Open the completed green workflow run.
5. Under **Artifacts**, download `AegisClient-2.0.0-Minecraft-26.1.2`.
6. Extract the downloaded artifact ZIP. It contains `aegis-client-2.0.0.jar`.

## Exact build command used by GitHub

From the repository root, after Java 25 and Gradle 9.8.2 are available:

```bash
gradle --no-daemon clean build
```

The workflow performs that command for you on GitHub's hosted runner, so Java and Gradle do not need to be installed on the PS5 just to compile the JAR.

## Important build status note

A GitHub Actions workflow is a build attempt, not a guarantee that the current source compiles. If it fails, open the failed run, expand the **Compile and package the mod** step, and share the complete error text. A successful build is indicated by a green run and the uploaded JAR artifact. The JAR still needs to be tested in Minecraft 26.1.2 before it should be called a verified release.
