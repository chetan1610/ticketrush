from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_url: str
    catalog_url: str = "http://127.0.0.1:8001"
    jwt_secret: str
    internal_token: str
    hold_minutes: int = 10

    model_config = SettingsConfigDict(env_file=".env")


settings = Settings()
