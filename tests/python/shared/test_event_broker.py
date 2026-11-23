import pytest

from shared.events.broker import _extract_event_id


class FakeMessage:
    def __init__(self, body=b"{}", headers=None, message_id=None):
        self.body = body
        self.headers = headers or {}
        self.message_id = message_id


@pytest.mark.unit
@pytest.mark.asyncio
async def test_extract_event_id_prefers_message_header():
    message = FakeMessage(
        body=b'{"event_id":"body-id"}',
        headers={"eventId": "header-id"},
        message_id="message-id",
    )

    assert _extract_event_id(message) == "header-id"


@pytest.mark.unit
@pytest.mark.asyncio
async def test_extract_event_id_falls_back_to_json_and_delivery_id():
    assert _extract_event_id(FakeMessage(body=b'{"event_id":"body-id"}')) == "body-id"
    assert _extract_event_id(FakeMessage(body=b"invalid", message_id="message-id")) == "message-id"
