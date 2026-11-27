# Plan 001 — Feed en vivo del vendedor y notificación out-of-band

## 1. Resumen

Dos entradas independientes: un consumidor Kafka (`notifications.feed`) que registra eventos de
pedido y los empuja por SSE, y un consumidor RabbitMQ de `seller.notify` que registra una entrega
out-of-band. La tabla `feed_events` da idempotencia, replay acotado y base para el SSE; la tabla
`deliveries` da idempotencia a la notificación. Cubre RF-1 … RF-16.

## 2. Dependencias y configuración

- `spring-kafka` (ya presente `spring-boot-starter-amqp`).
- `application.yaml` ya trae Kafka y RabbitMQ; agregar `app.feed.replay-size=100` y
  `app.feed.timeout-ms=1800000`.
- Constantes: topic `orders.events`, grupo `notifications.feed`, cola `seller.notify`,
  routing key `order.placed`, DLQ `seller.notify.dlq` (contrato de `infra`; no se declara).

## 3. Esquema Flyway

`V1__create_notification_tables.sql`:

- `feed_events`:
  - `event_id UUID PRIMARY KEY`
  - `order_id UUID NOT NULL`
  - `tenant_id UUID NOT NULL`
  - `event_type VARCHAR(50) NOT NULL`
  - `occurred_at TIMESTAMPTZ NOT NULL`
  - `payload JSONB NOT NULL`
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
  - índice `(tenant_id, created_at DESC)`.
- `deliveries`:
  - `message_id VARCHAR(100) PRIMARY KEY`
  - `order_id UUID NULL`
  - `tenant_id UUID NULL`
  - `channel VARCHAR(20) NOT NULL` (`OUT_OF_BAND`)
  - `status VARCHAR(20) NOT NULL`
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`.

## 4. Feed SSE

- `OrderFeedListener` `@KafkaListener(topics="orders.events", groupId="notifications.feed")`:
  - parsea el envelope, valida `eventId`/`orderId`/`tenantId`; si falta, log y retorno;
  - inserta en `feed_events` con `ON CONFLICT (event_id) DO NOTHING`;
  - publica el evento a los suscriptores en memoria del `tenantId`;
  - con `AckMode.RECORD`, el ACK ocurre después del commit.
- `FeedController` `GET /notifications/feed?tenantId=`:
  - `SseEmitter` con timeout configurable;
  - al conectar envía los últimos `replay-size` eventos del tenant desde `feed_events`;
  - `FeedBroker` mantiene emisores por tenant y elimina al completar/error.
- Los eventos se emiten con `id=eventId`, `event=eventType` y JSON del payload.

## 5. Notificación out-of-band

- `SellerNotificationListener` `@RabbitListener(queues="seller.notify")` con ACK manual.
- `RetryAttempts` cuenta `x-death` como en `order-api`; menos de 5 → NACK sin reencolar.
- Al quinto fallo publica el mensaje original en `seller.notify.dlq` y hace ACK.
- `SellerNotificationService`:
  - calcula un `messageId` estable (`messageId` header o `eventId` del payload);
  - si ya existe en `deliveries`, hace ACK sin repetir;
  - registra la entrega y delega en `OutOfBandSender` (adaptador que hoy loguea; email queda detrás
    de una interfaz para no inventar credenciales).

## 6. Paquetes (Layered)

- `messaging/kafka/listener/`, `model/`, `codec/`.
- `messaging/rabbit/listener/`, `RetryAttempts`, `publisher/DeadLetterPublisher`.
- `service/FeedService`, `service/SellerNotificationService`.
- `repository/FeedEventRepository`, `repository/DeliveryRepository`.
- `controller/FeedController`, `config/FeedBroker`.
- `client/` solo para el sender out-of-band.

## 7. Verificación

- Unitarias de codec, idempotencia de feed y de entrega, conteo de `x-death`.
- Web test del endpoint SSE (con `MockMvc` y emisor de prueba).
- `./mvnw verify`.

## 8. Trazabilidad RF → plan

| RF | Sección |
|---|---|
| RF-1…RF-3 | §2, §4 |
| RF-4, RF-5, RF-6 | §4 |
| RF-7…RF-10 | §5 |
| RF-11 | §4, §5 |
| RF-12 | §4 |
| RF-13, RF-14 | §3 |
| RF-15, RF-16 | §2 |
