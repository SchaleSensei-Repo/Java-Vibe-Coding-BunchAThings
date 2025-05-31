# Java Vibe Coding: BunchAThings

> A collection of small Java applications, mostly generated with AI and tweaked for learning, fun, and experimentation.

---

## 🚩 Badges

![License: Public Domain](https://img.shields.io/badge/license-public%20domain-brightgreen)
![Java](https://img.shields.io/badge/language-Java-yellow)


---

## 📋 Table of Contents

- [About](#about)
- [Features](#features)
- [Available Applications](#available-applications)
- [Planned/Future Apps](#plannedfuture-apps)
- [CI/CD Integration](#cicd-integration)
- [License](#license)
- [Contributing](#contributing)

---

## About

**Java Vibe Coding: BunchAThings** is a playground for hobby Java projects, most of which are AI-generated and then customized.  
This repository is intended for learning, experimentation, and sharing small, fun, and practical Java applications and for me to applying CI/CD stuff.

---

## Features

- 📦 **Application Hub:**  
  - Browse and launch all `.jar` applications from a single place.
  - Remembers last opened location.
  - Page navigation and debugging tools.

- 🛠️ **CI/CD Pipeline:**  
  - Automated build, test (soon), and release using Jenkins (see [CI/CD Integration](#cicd-integration)).
  - Dependency management via `libs/` directory.
  - Automatic packaging and GitHub release creation.

---

## Available Applications

<details>
  <summary><strong>1. 1000 Miles Card Game <em>(Fun/Games)</em></strong></summary>
  - Solo or hotseat multiplayer
  - Customizable settings
</details>

<details>
  <summary><strong>2. Bingo <em>(Fun/Games)</em></strong></summary>
  - Scoring system
  - Solo or hotseat multiplayer
  - Customizable settings
</details>

<details>
  <summary><strong>3. Hangman <em>(Fun/Games)</em></strong></summary>
  - Unlimited attempts
  - Custom wordlists (.txt/.json)
  - Remembers last wordlist location
</details>

<details>
  <summary><strong>4. Wordle <em>(Fun/Games)</em></strong></summary>
  - Unlimited attempts
  - Custom wordlists (.txt/.json)
  - Remembers last wordlist location
  - Not limited to 5-letter words
</details>

<details>
  <summary><strong>5. Minesweeper with a Twist <em>(Fun/Games)</em></strong></summary>
  - Scoring system (mines don’t end the game)
  - Customizable field and mine count
  - Negative scoring possible
</details>

<details>
  <summary><strong>6. Battleship <em>(Fun/Games)</em></strong></summary>
  - Customizable field size, ship quantity and length
  - Customizable fire amount per turn
  - Hotseat multiplayer with randomized ship positions
</details>

<details>
  <summary><strong>7. Deal or No Deal <em>(Fun/Games)</em></strong></summary>
  - Customizable bags quantity, bias and prize ranges
  - Customizable banker offering frequency, bias and prize range
  - Option to change bags mid-game
  - Track or hide bag values
  - Authentic mode that mimics the real-life game show
</details>

<details>
  <summary><strong>8. Slot Machine <em>(Fun/Games)</em></strong></summary>
  - Customizable starting points, payment, multipliers, and payout lines
  - Winning logs
</details>

<details>
  <summary><strong>9. Shop Budgeter</strong></summary>
  - Calculates what you can buy with a given budget
  - Supports custom items
  - Saves and auto-loads items/budgets
</details>

<details>
  <summary><strong>10. Random List Picker</strong></summary>
  - Uses custom .txt lists (can load multiple)
  - Randomly selects and displays results from loaded lists
</details>

<details>
  <summary><strong>11. Random Number Generator</strong></summary>
  - Custom min/max range
  - Optional bias
  - Logs and totals
</details>

<details>
  <summary><strong>12. Score Tracking</strong></summary>
  - Custom initial/additional/subtraction scores
  - Logs and totals
</details>

<details>
  <summary><strong>13. Warp Game (Snake and Ladder variant) <em>(Fun/Games)</em></strong></summary>
  - Score and lives system
  - Customizable initial score and life
  - Solo or multiplayer (hotseat, human or computer)
  - Customizable tile effects, dice, and field size
  - Local leaderboard
</details>

<details>
  <summary><strong>14. Java Converter</strong></summary>
  - Convert `.java` files to `.txt` and vice versa
  - Batch conversion
  - Remembers last input/output location
</details>

<details>
  <summary><strong>15. JSON Beautify and Compare</strong></summary>
  - Beautify and compare JSON files or pasted content
  - Syntax highlighting for differences
  - Remembers last file location
</details>

<details>
  <summary><strong>16. Investment Contribute Calculator</strong></summary>
  - Calculate investments based on monthly/yearly contributions (including negative values)
  - Simple calculation model
</details>

<details>
  <summary><strong>17. Investment Type Calculator</strong></summary>
  - Calculate investments based on type (stocks, bonds, portfolios, etc.)
  - Supports single/multiple income goals and investment types
  - Simple calculation model
</details>

<details>
  <summary><strong>18. Nuclear Casualty Estimate Calculator </strong></summary>
  - Calculate the casualty based on nuclear warhead impact or radiation levels
  - Have option to enable/disable nuclear winter and humanitarian aid
  - The output result have some description and calculation result table
  - The calculation is only based on rough estimate and not reflect on real scientific calculation
</details>

---

## Planned/Future Apps

<details>
  <summary><strong>1. Bejeweled/match-3 <em>(Fun/Games)</em></strong></summary>
  - Planned application
</details>

<details>
  <summary><strong>2. Sudoku <em>(Fun/Games)</em></strong></summary>
  - Solver for blank or custom grids
  - Supports various grid sizes (2x2, 3x2, etc.)
</details>

<details>
  <summary><strong>3. Memory Match <em>(Fun/Games)</em></strong></summary>
  - Number/symbol-based memory match
  - Customizable field sizes and scoring
  - Solo or up to 8-player hotseat
</details>

_Any suggestions welcome!_

---

## CI/CD Integration

This project uses a Jenkins-based CI/CD pipeline, as defined in the [Jenkinsfile](Jenkinsfile):

- **Build Automation:**  
  - Recursively finds all Java files with a `public static void main` method.
  - Compiles each application with dependencies from the `libs/` directory.
  - Packages each app as a `.jar` in the `out/` directory.

- **Release Packaging:**  
  - Collects all built `.jar` files into a `release_package/` directory.
  - Compresses the release package as a `.zip` archive.

- **GitHub Release Automation:**  
  - Automatically creates a new GitHub release for each build.
  - Uploads the zipped release package to GitHub using the provided credentials.

- **Notifications:**  
  - Sends email notifications on build success or failure.

**Pipeline Highlights:**
- [Jenkinsfile](Jenkinsfile) is fully declarative and supports parallel builds.
- External dependencies are managed via the `libs/` directory.
- Release artifacts are versioned and published automatically.

---

## License

This project is licensed under [The Unlicense](LICENSE) — public domain dedication.

**You are free to:**
- Clone, modify, distribute, and use this code for any purpose, commercial or non-commercial, without any restrictions.

**No restrictions. No warranty.**  
See the [LICENSE](LICENSE) file for more details.

---

## Contributing

Contributions are very welcome!

1. **Fork** this repository.
2. **Create a branch** for your feature or fix.
3. **Make your changes** and commit with clear messages.
4. **Open a Pull Request** describing your changes.
5. The maintainer will review and merge if everything looks good.

You can also open Issues for bugs, ideas, or questions.

---

**Enjoy exploring these Java mini-projects!**
