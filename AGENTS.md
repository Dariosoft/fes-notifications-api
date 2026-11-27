# AGENTS.md - notifications-api

## Proyecto
API de notificaciones de Friendly E-Shop. Es un servicio independiente en Java 25 y Spring Boot que
posee el feed en vivo del vendedor y la entrega de notificaciones; expone sus contratos bajo
`/notifications`.
Consume eventos de dominio del pedido desde Kafka para el feed en vivo y desde RabbitMQ
(`seller.notify`) para la notificación out-of-band. Persiste únicamente en la base `notifications`,
usa Flyway y emite telemetría mediante Actuator/OpenTelemetry. No es fuente de verdad de pedidos,
productos, pagos ni cuentas.

## Comandos
- Ejecutar: `./mvnw spring-boot:run`
- Tests: `./mvnw test`
- Compilar y verificar: `./mvnw verify`
- Cobertura: `./mvnw verify` genera `target/site/jacoco/jacoco.xml`, que SonarQube lee por defecto.
- SonarQube: `./mvnw sonar:sonar` con `SONAR_TOKEN` en el entorno; el `projectKey` y la organización están en `pom.xml`. En CI se ejecuta en cada evento y falla si no pasa el Quality Gate.
- SBOM: `./mvnw cyclonedx:makeBom` genera `target/bom.json`; el CI lo escanea con Trivy.
- Hook local: activar una vez por clon con `git config core.hooksPath .githooks`; bloquea commits con credenciales obvias.
- Lint: `./mvnw checkstyle:check`; también se ejecuta automáticamente en la fase `validate`.

## Estilo y convenciones
- Usa Java 25, Spring Boot 4.1 y el paquete `com.friendlyeshop.notifications`.
- Nombres, código y documentación técnica en inglés; mensajes visibles al usuario en español.
- Respeta `checkstyle.xml`: 4 espacios, sin tabs, líneas de hasta 120 caracteres e imports explícitos.
- Mantén controladores delgados y separa el transporte del broker de la entrega al vendedor.
- Crea nuevas migraciones Flyway; no edites migraciones ya aplicadas. Hibernate solo valida el esquema.

## Reglas
- Lee la spec activa, si existe, antes de tocar código.
- Para implementar usa solo `/spring-boot-project-creator` en su opción Layered y `/clean-code-guard`.
- La opción Layered organiza el paquete `com.friendlyeshop.notifications` en `controller/`,
  `service/`, `repository/`, `model/`, `model/dto/`, `config/` y `exception/`.
- Usa `/spring-boot-layered-template` como complemento de la opción Layered cuando una clase no
  encaje claramente en las carpetas básicas o al decidir si corresponde crear paquetes como
  `messaging/`, `event/`, `client/`, `mapper/` o `validation/`.
- No uses la opción DDD de esa skill ni regeneres el proyecto con Spring Initializr al modificar este servicio.
- Este servicio es un consumidor de solo lectura: nunca escribe pedidos, pagos, productos ni cuentas.
- No entregues la misma notificación por Kafka y RabbitMQ: el feed en vivo sale de Kafka y la
  notificación out-of-band de `seller.notify`; son canales distintos.
- Los consumidores deben ser idempotentes y tolerar reentregas; no avances el offset de Kafka si la
  entrega no quedó registrada.
- Conserva `/notifications`, las variables de entorno, los health checks y las métricas usadas por Kubernetes.
- No omitas ni desactives reglas de Checkstyle para evitar corregir una violación.
- Los manifiestos y secretos pertenecen a `infra`; coordina allí cambios de puerto, ruta o configuración.

## Al terminar cualquier tarea
- Tras cambios no triviales de código de producción, aplica `/clean-code-guard` antes de finalizar.
- Ejecuta `./mvnw verify`; incluye Checkstyle y los tests.
- Prueba reentregas, desconexiones del feed y reintentos de notificación; añade migraciones para cambios de esquema.
- Comprueba que no se hayan roto `/notifications` ni los endpoints de Actuator.
