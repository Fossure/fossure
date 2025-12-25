import httpx
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.settings import settings

bearer_scheme = HTTPBearer(auto_error=False)


def _gotrue_url() -> str:
    return settings.gotrue_url.rstrip("/")


async def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> dict:
    if credentials is None or not credentials.credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing bearer token",
        )

    token = credentials.credentials
    async with httpx.AsyncClient(timeout=5.0) as client:
        response = await client.get(
            f"{_gotrue_url()}/user",
            headers={"Authorization": f"Bearer {token}"},
        )

    if response.status_code != status.HTTP_200_OK:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )

    return response.json()
