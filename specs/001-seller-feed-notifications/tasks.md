# Tasks 001 — Feed en vivo y notificación out-of-band

## Fase A — Base y esquema

- [x] **T1. Agregar `spring-kafka` y configuración.** Cubre RF-1, RF-16.
  Done when: `./mvnw compile` pasa con Kafka y RabbitMQ configurados.
- [x] **T2. Crear `V1__create_notification_tables.sql` y entidades.** Cubre RF-2, RF-8, RF-13.
  Done when: Flyway aplica e Hibernate valida `feed_events` y `deliveries`.

## Fase B — Feed

- [x] **T3. Codec del envelope y `FeedEventRepository`.** Cubre RF-2, RF-3.
  Done when: inserta idempotente por `eventId`.
- [x] **T4. Implementar `OrderFeedListener`.** Cubre RF-1, RF-2, RF-3, RF-12.
  Done when: persiste y publica al broker en memoria sin confirmar antes del commit.
- [x] **T5. Implementar `GET /notifications/feed` (SSE).** Cubre RF-4, RF-5, RF-6.
  Done when: envía replay y luego eventos en vivo; cierra al desconectar.

## Fase C — Notificación out-of-band

- [x] **T6. Implementar `SellerNotificationListener`.** Cubre RF-7, RF-8, RF-10.
  Done when: registra la entrega, ACK manual y conteo de `x-death` hasta DLQ.
- [x] **T7. Idempotencia de entregas.** Cubre RF-9, RF-11.
  Done when: una reentrega no repite la notificación registrada.
- [x] **T8. Adaptador `OutOfBandSender`.** Cubre RF-7, RF-11.
  Done when: la entrega se delega en una interfaz y el adaptador por defecto loguea.

## Fase D — Pruebas y cierre

- [x] **T9. Unitarias y web test del SSE.** Cubre RF-2…RF-10.
  Done when: cubren duplicados, replay, reconexión y DLQ.
- [x] **T10. `./mvnw verify` y demo manual.** Cubre RF-13…RF-16.
  Done when: el feed muestra el evento y `seller.notify` registra una entrega.
