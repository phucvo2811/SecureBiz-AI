from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="AI_", case_sensitive=False)

    service_name: str = "securebiz-ai-service"
    model_dir: Path = Path("models/artifacts")
    default_model: str = "isolation_forest"
    random_seed: int = 42
    internal_api_key: str = Field(default="replace-with-random-service-credential", validation_alias="INTERNAL_AI_API_KEY")


@lru_cache
def get_settings() -> Settings:
    return Settings()
