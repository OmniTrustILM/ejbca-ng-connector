# CLAUDE.md

Engineering and process guidance for this connector. Build commands, architecture, and
package layout are derivable from the code and README; this file captures what is **not**
obvious from the code.

## Dependency resolution, snapshots & the x509-common-util jar

- Most dependencies (the `com.otilm:dependencies` parent BOM and its transitives) resolve
  from **Maven Central**.
- **`com.otilm:interfaces` is currently consumed as a SNAPSHOT** (`2.18.1-SNAPSHOT`), so the
  build needs the snapshot `<repositories>` declared in `pom.xml`: GitHub Packages
  (`maven.pkg.github.com/omnitrustilm/*`) and the public Sonatype Central Portal snapshots
  (`central.sonatype.com/repository/maven-snapshots`). The snapshot resolves from
  `central-portal` without auth; the `github` repo is best-effort and needs a token. **Builds
  are not reproducible** until `com.otilm:interfaces` cuts a release — pin it then.
- **`com.keyfactor:x509-common-util` is NOT on any public Maven repository.** It ships as a jar
  under `ejbca-libs/` and is installed into the local repo by `ejbca-libs/maven-install-files.sh`.
  Run that script before any Maven build. CI (`build.yml` / `build_pr.yml`) and the Dockerfile
  build stage all run it. **Do not delete the script or the jar.**
- The Docker publish/test workflows call the shared OmniTrustILM workflow with
  **`pre-build: none`**: the self-building Dockerfile runs the install script in its build stage.
  `pre-build: maven` would run a plain `mvn package` that cannot install the local jar.

## Namespace

- Group/package is `com.otilm` throughout: the Maven parent is `com.otilm:dependencies`, the
  interfaces dependency is `com.otilm:interfaces`, and the connector's own code lives under
  `com.otilm.ca.connector.ejbca`. The legacy namespace has been fully removed.

## EJBCA SOAP

- The `EjbcaWS` port is generated from `ejbcaws.wsdl`. Its SOAP `targetNamespace`
  `http://ws.protocol.core.ejbca.org/` belongs to **EJBCA, not us** — never change it.
  It contains no `com.otilm` package token, so a package-rename find/replace skips it automatically.
- The `EjbcaWS` port is cached per authority (`connectionsCache`) and reused across requests.
  JAX-WS timeouts are applied to the binding request context (`com.sun.xml.ws.connect.timeout` /
  `com.sun.xml.ws.request.timeout`).

## Don't leak EJBCA internals to the wire

- Don't forward raw EJBCA / `Exception.getMessage()` to clients beyond what `ExceptionHandlingAdvice`
  already maps. Note there is **no Spring Security web filter chain** here, so `AccessDeniedException`
  (and other `RuntimeException`s) map to **HTTP 400**, not 403.

## Test coverage & Sonar

- Overall coverage is low: the connector is mostly generated JAX-WS stubs (`ws/`, ~190 classes)
  plus integration-heavy service code that talks to EJBCA over SOAP/REST. End-to-end coverage
  comes from `EJBCAIT`, which needs a live EJBCA and does not run in CI.
- `sonar.coverage.exclusions` / `sonar.cpd.exclusions` (in `pom.xml`) exclude the generated stubs,
  DTOs, JPA entities/repositories, Spring config, enums, exception types, thin API controllers,
  and Flyway migrations. The **service layer is deliberately not excluded** — low coverage there is
  real test debt. SonarCloud gates on *new-code* coverage, so a large mechanical change touching
  the untested service layer will show low new-code coverage.

## Quality gate before pushing

Run locally before opening a PR (GitHub Actions then run the authoritative SonarCloud + Trivy +
CodeQL checks):
- `./scripts/sonar-local.sh` — ephemeral SonarQube smoke check (issues on changed files).
- `trivy` with `config/trivy.yaml` against the built artifact — no CRITICAL vulnerabilities.
- `copilot --allow-all-tools -p "..."` — an independent review pass on the diff.

## Commits & PRs

Write a plain description of what changed — **no** co-author/attribution trailers, and **no**
validation/quality-status lines (test counts, "BUILD SUCCESS", coverage numbers).
