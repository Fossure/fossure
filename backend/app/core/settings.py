from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_url: str = "postgresql+asyncpg://fossure:fossure@localhost:5432/fossure"
    gotrue_url: str = "http://fossure-auth:9999"

    model_config = SettingsConfigDict(env_prefix="", case_sensitive=False)


settings = Settings()
