GitHub Pages deployment

This project is configured to deploy the :web Kotlin/JS app to GitHub Pages using GitHub Actions.

Steps to enable:
1) Push the repository to GitHub.
2) In GitHub repository Settings → Pages:
   - Source: GitHub Actions
3) Ensure default branch is `main` or `master` (workflow triggers on both).
4) Commit changes in `web/` or trigger `Run workflow` manually from the Actions tab.
5) After the workflow succeeds, your site will be available at:
   - https://<your-username>.github.io/<repo-name>/

Notes
- The workflow builds the production bundle with `:web:jsBrowserProductionWebpack` and publishes the `web/build/dist/js/productionExecutable` folder.
- If your repository name is not the site root (user/organization site), assets are emitted with relative paths suitable for project pages.
- If you customize output paths or base href, adjust the workflow `Prepare Pages artifact` step accordingly.
