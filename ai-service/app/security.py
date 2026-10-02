import secrets
from typing import Annotated

from fastapi import Header, HTTPException, status

from app.config import get_settings


def require_internal_api_key(
    supplied_key: Annotated[str | None, Header(alias="X-Internal-Api-Key")] = None,
) -> None:
    expected_key = get_settings().internal_api_key
    if supplied_key is None or not secrets.compare_digest(supplied_key, expected_key):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid service credential",
        )
