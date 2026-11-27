# Manual setup — notifications-api

These steps live outside the repository because they need organization or SonarCloud
permissions that CI must not handle with hardcoded credentials.

## SonarCloud

1. In the `dariosoft` SonarCloud organization, create the project
   `Dariosoft_fes-notifications-api` and bind it to `Dariosoft/fes-notifications-api`.
2. Disable *Automatic Analysis* for the project; this repository runs CI-based analysis.
3. Make sure the organization secret `SONAR_TOKEN` is shared with this repository
   (GitHub/Dariosoft/Actions secrets). Without it the `SonarQube Cloud analysis` step fails.
4. The project key and organization are already set in `pom.xml`
   (`Dariosoft_fes-notifications-api`, `dariosoft`).

## GitHub project board

`Auto Add to Project` uses the organization secret `ADD_TO_PROJECT_PAT`. Share that
secret with this repository. If it is not shared, the workflow skips cleanly instead of
failing, and issues are simply not synchronized with the board.

## Runtime

Kafka bootstrap servers, RabbitMQ, the notifications database and secrets are owned by
`fes-infra`. This service only reads `KAFKA_BOOTSTRAP_SERVERS`, `RABBITMQ_*` and
`DATABASE_*`.
