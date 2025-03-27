# ProofIt – Automated Proof-of-Concept Generator

## 5CCS2SEG - Major Group Project: Team Runtime Terrors

---

## Overview

**ProofIt** is an AI-powered automatic proof-of-concept (PoC) generator designed to transform natural language business requirements into functional PoC applications. Utilizing Generative AI, users can simply input a descriptive prompt, and ProofIt generates a corresponding PoC to fulfill the described business need.

## Application Architecture

The application is structured into two primary components:

- **Frontend (Client)**: Responsible for capturing user inputs, communicating with the backend API, and presenting generated results.
- **Backend (Server)**: Provides a robust REST API, processes incoming data, and generates PoCs. It integrates an embedding microservice, which performs similarity searches to retrieve suitable templates essential for code generation.

## Project Client

This project was developed in collaboration with **Amazon Web Services (AWS)** as an Amazon Capstone Project. The development team closely collaborated with AWS representatives **Nathaniel Richard Powell** and **Patrick Bradshaw** throughout the development lifecycle.

## Live Deployment

Explore the live version at: [proofit.uk](https://proofit.uk)

## Development Team

- Enzo Bestetti
- Isabella McLean
- Jacelyne Tan
- Krystian Augustynowicz
- Lucia Garces Gutierrez
- Markus Meresma
- Nischal Gurung
- Reza Samaei
- Usman Khan
- Wealthie Tjendera

## Documentation

Detailed project documentation, diagrams, and additional resources can be found in the **`one-day-poc-docs`** directory. The provided resources include:

- **Developer’s Manual**:
  - Installation instructions
  - Local execution guide
  - Automated testing instructions
  - Code coverage reporting
  - Comprehensive explanations of the project structure and functionality
  - Fully documented components with illustrative code examples

- **System Diagrams**: Visual representations of the system's architecture and workflows.

- **Project Report**: Contextual insights on project rationale, development challenges, and key lessons learned (intended primarily for academic context).

## Sources & References

The following sources significantly contributed to the project. All AI-generated code has been adapted and refined (never copied verbatim):

- ChatGPT
- Claude
- GitHub Copilot
- Stack Overflow
- Kotlin Documentation
- React Documentation

External libraries and dependencies are explicitly declared and documented within:
- `build.gradle.kts` (top-level)
- Module-specific `build.gradle.kts`
- Python microservice dependencies listed in `requirements.txt`.

