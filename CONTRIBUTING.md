# Contributing

Thanks for your interest in SwipeFit! Bug reports, ideas and pull requests are welcome.

## Getting started

1. Fork the repo and clone your fork.
2. Follow [docs/SETUP.md](docs/SETUP.md) to connect your own Firebase project.
3. Create a branch: `git checkout -b feature/short-description`.

## Guidelines

- **Architecture:** keep to MVVM. Firebase calls go in `data/` repositories, screen state goes in a ViewModel, and composables only render state. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
- **Styling:** use colors, typography and components from `ui/theme` and `ui/common` rather than hard-coding values.
- **Code style:** official Kotlin style (`kotlin.code.style=official`); Android Studio's **Reformat Code** keeps it consistent.
- **Security rules:** if you change the data model, update `firestore.rules` / `storage.rules` and the tables in the architecture doc in the same PR.
- **Secrets:** never commit `google-services.json`, keystores, or API keys.

## Commit messages

Use short, imperative messages such as `Add podium animation` or `Fix crash when group is deleted`.

## Pull requests

Describe what changed and why, and include a screenshot for UI changes. Confirm the app builds and that you tested it on an emulator or a device.
