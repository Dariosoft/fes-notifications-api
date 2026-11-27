# Spec 001 — Feed en vivo del vendedor y notificación out-of-band

## Contexto y objetivo

El vendedor necesita enterarse de las ventas en tiempo real y recibir una notificación durable
fuera de la aplicación. Este servicio entrega el feed en vivo por SSE desde Kafka y la notificación
out-of-band desde la cola RabbitMQ `seller.notify`, sin duplicar el mismo canal y sin ser dueño del
pedido.

## Usuarios / actores

- Vendedor que mira el feed del panel y recibe notificaciones.
- `panel-api`, que proxya el feed.
- `order-api`, productor de `order.placed`.
- `infra`, que declara topic, cola y reintentos.

## Historias de usuario

- H1: Como vendedor quiero ver los pedidos nuevos en vivo en el panel.
- H2: Como vendedor quiero recibir una notificación durable fuera del panel.
- H3: Como operador quiero que una reentrega no duplique el feed ni la notificación.
- H4: Como operador quiero que al reconectar el SSE se muestren los últimos eventos.

## Requisitos funcionales (criterios de aceptación en EARS)

- RF-1: EL SISTEMA consumirá `orders.events` con el grupo `notifications.feed`.
- RF-2: CUANDO reciba un evento utilizable, EL SISTEMA lo registrará en el feed de su tienda.
- RF-3: SI recibe un `eventId` ya registrado, ENTONCES EL SISTEMA no duplicará el evento del feed.
- RF-4: EL SISTEMA expondrá `GET /notifications/feed` como `text/event-stream` filtrado por `tenantId`.
- RF-5: CUANDO un cliente se conecte al feed, EL SISTEMA le enviará los últimos eventos registrados de su tienda y luego los nuevos.
- RF-6: EL SISTEMA mantendrá las conexiones SSE abiertas hasta que el cliente se desconecte.
- RF-7: EL SISTEMA consumirá la cola RabbitMQ `seller.notify` para la notificación out-of-band.
- RF-8: CUANDO procese un mensaje de `seller.notify`, EL SISTEMA registrará la entrega y confirmará la recepción solo después.
- RF-9: SI el mensaje se reentrega, ENTONCES EL SISTEMA no repetirá la notificación ya registrada.
- RF-10: SI el procesamiento falla, ENTONCES EL SISTEMA no confirmará el mensaje mientras queden reintentos y, agotados los 5 intentos, lo enviará a la cola de descarte y confirmará.
- RF-11: EL SISTEMA no entregará la misma notificación por Kafka y por RabbitMQ: el feed sale de Kafka y la notificación out-of-band de `seller.notify`.
- RF-12: EL SISTEMA confirmará el offset de Kafka solo después de persistir el evento del feed.
- RF-13: EL SISTEMA persistirá únicamente en la base `notifications`.
- RF-14: EL SISTEMA no escribirá tablas de pedidos, pagos, productos ni cuentas.
- RF-15: EL SISTEMA conservará los health checks de Actuator.
- RF-16: EL SISTEMA obtendrá brokers, base y RabbitMQ desde `KAFKA_BOOTSTRAP_SERVERS`, `DATABASE_*` y `RABBITMQ_*`.

## Requisitos no funcionales

- Idempotencia por `eventId` y por identificador de mensaje RabbitMQ.
- SSE sin buffering.
- Replay acotado de los últimos eventos al reconectar.
- Sin duplicación entre canales.

## Casos límite

- Evento duplicado: no duplica feed.
- Reconexión SSE: recibe últimos eventos sin duplicados.
- Sin suscriptores: los eventos quedan registrados para el replay.
- Mensaje RabbitMQ duplicado: no repite entrega.
- Falla transitoria: reintento y DLQ al quinto intento.
- Evento sin `tenantId`: se descarta con log.

## Fuera de alcance

- Timeline y métricas.
- Publicar pedidos.
- Declarar topics, colas o DLQ.
- Proxy del panel.
- Canales de entrega reales (email, push) más allá de un adaptador registrado.

## Criterios de finalización

- `./mvnw verify` en verde.
- Demo: publicar `order.placed` y verlo en el SSE; reenviar y no duplicar; publicar en `seller.notify` y ver la entrega registrada.

## Dudas abiertas

Ninguna.
