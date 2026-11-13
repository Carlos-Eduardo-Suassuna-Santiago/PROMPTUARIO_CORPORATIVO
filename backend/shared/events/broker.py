"""
Async RabbitMQ publisher/consumer using aio-pika.
Each microservice uses these base classes.
"""
from __future__ import annotations

import asyncio
import json
import logging
from collections import deque
from typing import Callable, Awaitable, Type

import aio_pika
from aio_pika import ExchangeType, Message, DeliveryMode
from aio_pika.abc import AbstractRobustConnection

from shared.events import DomainEvent, EXCHANGES

logger = logging.getLogger(__name__)


class EventPublisher:
    """Publishes domain events to RabbitMQ topic exchanges."""

    def __init__(self, url: str):
        self._url = url
        self._connection: AbstractRobustConnection | None = None
        self._channel = None

    async def connect(self) -> None:
        backoff = 1.0
        while True:
            try:
                self._connection = await aio_pika.connect_robust(self._url)
                self._channel = await self._connection.channel()
                # Declare all exchanges on startup
                for exchange_name, exchange_type in EXCHANGES.items():
                    await self._channel.declare_exchange(
                        exchange_name,
                        ExchangeType.TOPIC if exchange_type == "topic" else ExchangeType.DIRECT,
                        durable=True,
                    )
                logger.info("EventPublisher connected to RabbitMQ")
                return
            except Exception as e:  # retry until RabbitMQ becomes available
                logger.warning("EventPublisher cannot connect to RabbitMQ (%s). Retrying in %.0fs...", e, backoff)
                await asyncio.sleep(backoff)
                backoff = min(backoff * 2, 30.0)

    async def publish(self, event: DomainEvent) -> None:
        if not self._channel:
            raise RuntimeError("Publisher not connected")
        exchange_name = getattr(event, "EXCHANGE", "promptuario.iam")
        routing_key = getattr(event, "ROUTING_KEY", "event.generic")
        exchange = await self._channel.get_exchange(exchange_name)
        message = Message(
            body=event.to_json(),
            content_type="application/json",
            delivery_mode=DeliveryMode.PERSISTENT,
            message_id=event.event_id,
            correlation_id=event.correlation_id,
            headers={
                "eventId": event.event_id,
                "eventType": event.event_type,
                "timestamp": event.timestamp.isoformat(),
                "correlationId": event.correlation_id,
            },
        )
        await exchange.publish(message, routing_key=routing_key)
        logger.info(
            "event_published exchange=%s routingKey=%s eventId=%s eventType=%s correlationId=%s",
            exchange_name,
            routing_key,
            event.event_id,
            event.event_type,
            event.correlation_id,
        )

    async def close(self) -> None:
        if self._connection:
            await self._connection.close()


class EventConsumer:
    """Subscribes to domain events from RabbitMQ."""

    def __init__(self, url: str, service_name: str):
        self._url = url
        self._service_name = service_name
        self._connection: AbstractRobustConnection | None = None
        self._channel = None
        self._handlers: dict[str, tuple[str, Callable]] = {}
        self._processed_message_ids: set[str] = set()
        self._processed_message_order: deque[str] = deque()
        self._event_locks: dict[str, asyncio.Lock] = {}

    async def connect(self) -> None:
        backoff = 1.0
        while True:
            try:
                self._connection = await aio_pika.connect_robust(self._url)
                self._channel = await self._connection.channel()
                await self._channel.set_qos(prefetch_count=10)
                for exchange_name, exchange_type in EXCHANGES.items():
                    await self._channel.declare_exchange(
                        exchange_name,
                        ExchangeType.TOPIC if exchange_type == "topic" else ExchangeType.DIRECT,
                        durable=True,
                    )
                logger.info("EventConsumer connected to RabbitMQ")
                return
            except Exception as e:
                logger.warning("EventConsumer cannot connect to RabbitMQ (%s). Retrying in %.0fs...", e, backoff)
                await asyncio.sleep(backoff)
                backoff = min(backoff * 2, 30.0)

    def register(
        self,
        exchange: str,
        routing_key: str,
        handler: Callable[[bytes], Awaitable[None]],
    ) -> None:
        self._handlers[routing_key] = (exchange, handler)

    async def start(self) -> None:
        if not self._channel:
            raise RuntimeError("Consumer not connected")
        for routing_key, (exchange_name, handler) in self._handlers.items():
            queue_name = f"{self._service_name}.{routing_key.replace('.', '_')}"
            # Dead-letter queue
            dlx = await self._channel.get_exchange("promptuario.dlx")
            queue = await self._channel.declare_queue(
                queue_name,
                durable=True,
                arguments={
                    "x-dead-letter-exchange": "promptuario.dlx",
                    "x-dead-letter-routing-key": queue_name,
                },
            )
            exchange = await self._channel.get_exchange(exchange_name)
            await queue.bind(exchange, routing_key=routing_key)

            async def _make_callback(h: Callable, registered_routing_key: str):
                async def callback(message: aio_pika.IncomingMessage):
                    async with message.process(requeue=True):
                        event_id = _extract_event_id(message)
                        if not event_id:
                            try:
                                await h(message.body)
                            except Exception as e:
                                logger.error("Handler error for %s: %s", registered_routing_key, e)
                                raise
                            return

                        # Serialize concurrent deliveries of the same domain event. If
                        # the first handler fails, the waiting delivery can still run.
                        event_lock = self._event_locks.setdefault(event_id, asyncio.Lock())
                        async with event_lock:
                            if event_id in self._processed_message_ids:
                                logger.info(
                                    "event_duplicate_ignored routingKey=%s eventId=%s",
                                    registered_routing_key,
                                    event_id,
                                )
                                return
                            try:
                                await h(message.body)
                                self._mark_processed(event_id)
                            except Exception as e:
                                logger.error("Handler error for %s: %s", registered_routing_key, e)
                                raise
                return callback

            await queue.consume(await _make_callback(handler, routing_key))
            logger.info("Subscribed: %s → %s", queue_name, routing_key)

    def _mark_processed(self, event_id: str) -> None:
        # Keep idempotency memory bounded for long-running consumers.
        if event_id in self._processed_message_ids:
            return
        if len(self._processed_message_order) >= 10_000:
            oldest = self._processed_message_order.popleft()
            self._processed_message_ids.discard(oldest)
            self._event_locks.pop(oldest, None)
        self._processed_message_ids.add(event_id)
        self._processed_message_order.append(event_id)

    async def close(self) -> None:
        if self._connection:
            await self._connection.close()


def _extract_event_id(message: aio_pika.IncomingMessage) -> str | None:
    """Resolve the domain event id independently of RabbitMQ delivery metadata."""
    headers = message.headers or {}
    for key in ("eventId", "event_id"):
        value = headers.get(key)
        if value:
            return value.decode() if isinstance(value, bytes) else str(value)

    try:
        body = json.loads(message.body)
        value = body.get("eventId") or body.get("event_id")
        if value:
            return str(value)
    except (json.JSONDecodeError, TypeError, UnicodeDecodeError):
        pass

    return message.message_id
