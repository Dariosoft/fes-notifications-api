# Notifications API

Owns the seller live feed and durable seller notifications. It consumes domain events from Kafka
for the live feed and RabbitMQ (`seller.notify`) for out-of-band notifications. It is never the
source of truth for orders, payments, products or accounts.

## Implementation

Every implementation in this repository uses only these two skills:

- `spring-boot-project-creator`, Layered option. Place code under
  `com.friendlyeshop.notifications` in `controller/`, `service/`, `repository/`, `model/`,
  `model/dto/`, `config/`, and `exception/`. Do not use the DDD option of that skill, and do not
  bootstrap a new project with Spring Initializr when changing this service.
- `clean-code-guard` after every non-trivial production code change, before the work is considered
  done.

```bash
./mvnw test
docker build -t friendly-e-shop/notifications-api:dev .
```
