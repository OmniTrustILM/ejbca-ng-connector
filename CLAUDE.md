# CLAUDE.md

Engineering and process guidance for this connector. Build commands, architecture, and
package layout are derivable from the code and README; this file captures what is **not**
obvious from the code.

## Dependency resolution & the x509-common-util jar

- All released dependencies resolve from **Maven Central** — there is no `settings.xml` and
  no custom `<repositories>` (the parent `com.otilm:dependencies`, `com.czertainly:interfaces`,
  and all transitives are on Central).
- **`com.keyfactor:x509-common-util` is NOT on any public Maven repository.** It ships as a jar
  under `ejbca-libs/` and is installed into the local repo by `ejbca-libs/maven-install-files.sh`.
  Run that script before any Maven build. CI (`build.yml` / `build_pr.yml`) and the Dockerfile
  build stage all run it. **Do not delete the script or the jar**, and don't assume `mvn` works
  without it — "resolve from Maven Central" applies to the *released* deps only.
- Consequently the Docker publish/test workflows call the shared OmniTrustILM workflow with
  **`pre-build: none`**: the self-building Dockerfile runs the install script in its build stage.
  `pre-build: maven` would run a plain `mvn package` that cannot install the local jar.

## The namespace is transitional — don't "fix" it

- The Maven parent is `com.otilm:dependencies`, but the connector's own Java packages **and** all
  interface imports are still `com.czertainly`. That's because the released `interfaces` artifact
  (2.18.0) is still `com.czertainly`, and there is no released `com.otilm:interfaces` yet.
- This mixed state is intentional. **Do not rename the `com.czertainly` imports/packages to
  `com.otilm`** piecemeal. The full namespace move is a planned follow-up once a
  `com.otilm:interfaces` release exists, done atomically (and with a DB class-name migration
  check for any serialized FQCNs).

## EJBCA SOAP

- The `EjbcaWS` port is generated from `ejbcaws.wsdl`. Its SOAP `targetNamespace`
  `http://ws.protocol.core.ejbca.org/` belongs to **EJBCA, not us** — never change it during any
  CZERTAINLY→ILM rebrand (a blanket `com.czertainly`→`com.otilm` replace must skip it).
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
  real test debt, not an exclusion candidate. SonarCloud gates on *new-code* coverage, so a large
  mechanical change touching the untested service layer will show low new-code coverage.

## Quality gate before pushing

Run locally before opening a PR (GitHub Actions then run the authoritative SonarCloud + Trivy +
CodeQL checks):
- `./scripts/sonar-local.sh` — ephemeral SonarQube smoke check (issues on changed files).
- `trivy` with `config/trivy.yaml` against the built artifact — no CRITICAL vulnerabilities.
- `copilot --allow-all-tools -p "..."` — an independent review pass on the diff.

## Commits & PRs

Write a plain description of what changed — **no** co-author/attribution trailers, and **no**
validation/quality-status lines (test counts, "BUILD SUCCESS", coverage numbers).
