from fastapi import APIRouter, Depends

from app.auth.dependencies import get_current_user

router = APIRouter()


@router.get("/me")
async def read_current_user(user: dict = Depends(get_current_user)) -> dict:
    return user
