# Security policy

NotesTaking App is undergoing rebranding and correctness/security review. There are historical audit findings requiring verification and remediation. Passing CI does **not** mean the app has completed a security assessment.

## Reporting vulnerabilities

If GitHub private vulnerability reporting is enabled for this repository, use **Security → Advisories → Report a vulnerability** to disclose an issue privately. If that feature is unavailable, contact repository maintainers through an agreed private channel rather than posting exploit details or private documents publicly. No private contact address is currently published.

Please include the affected component, app version, non-sensitive reproduction steps and likely impact. Redact personal notes, signing keys, access tokens and document content.

## Scope

Important areas include PDF import and native parsing, ZIP/XML template handling, session save/restore, Android DocumentsProvider and SAF, third-party libraries, and the JNI interface. Security-sensitive changes should include regression tests and an Android validation step.

Original MIT and third-party license notices must remain present after fixes.
