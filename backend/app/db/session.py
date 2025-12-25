from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.core.settings import settings

DATABASE_URL = settings.database_url

engine = create_async_engine(DATABASE_URL, pool_pre_ping=True)
SessionLocal = async_sessionmaker(bind=engine, class_=AsyncSession, expire_on_commit=False)


async def get_db() -> AsyncSession:
    async with SessionLocal() as session:
        yield session


async def set_rls_claims(session: AsyncSession, claims: dict) -> None:
    subject = claims.get("sub")
    role = claims.get("role", "authenticated")
    if subject:
        await session.execute(
            text("select set_config('request.jwt.claim.sub', :sub, true)"),
            {"sub": subject},
        )
    await session.execute(
        text("select set_config('request.jwt.claim.role', :role, true)"),
        {"role": role},
    )
