# Security Policy

## Supported versions

| Version | Support |
| --- | --- |
| `v1.2.x` and the current development branch | Supported |
| Older releases | Best effort only |

The GitHub release APKs are debug testing artifacts. Production signing is not
configured in this repository, so do not treat a debug APK as a production
distribution channel.

## Reporting a vulnerability

Please do not report security vulnerabilities in a public issue, pull request,
discussion, or release comment.

Use GitHub's private vulnerability reporting or Security Advisories feature for
this repository when it is available. If that option is not available, contact
the repository maintainer privately through <https://github.com/rakshit-737> and
ask for a private security-reporting channel. Do not disclose sensitive details
publicly while waiting for a response.

Include as much of the following as is safe to share:

- A clear description of the vulnerability and affected component.
- The affected version, commit, device, and Android API level.
- Reproduction steps or a minimal proof of concept.
- Security impact and realistic attack prerequisites.
- Relevant logs or screenshots with credentials, tokens, personal data, and
  private package information removed.
- Any suggested mitigation or patch, if available.

Please allow reasonable time for investigation and remediation before public
disclosure. The maintainers will aim to acknowledge a report within seven days
and will provide updates when the impact and fix timeline are understood.

## Safe-harbor expectations

Good-faith research is welcome when it:

- Avoids accessing, changing, or deleting data that does not belong to you.
- Avoids denial-of-service, persistence, phishing, credential theft, or
  destructive testing.
- Uses test accounts, local builds, and isolated devices where possible.
- Stops after confirming the issue and reports the minimum evidence needed to
  reproduce it.
- Respects the privacy and safety of other users, maintainers, and services.

Testing third-party apps, Android services, GitHub infrastructure, or other
systems outside this repository is not authorized by this policy.

## Scope notes

The main security scope includes the Android application, its local persistence,
notification actions, repository automation, and the project website source.
Third-party dependencies and platform vulnerabilities should be reported to
their respective maintainers as well, while still mentioning relevant impact
on AdBreak Arcade when appropriate.
