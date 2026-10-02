from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class HealthResponse(BaseModel):
    status: str
    service: str


class ModelInfoResponse(BaseModel):
    available: bool
    model_version: str | None = None
    algorithm: str | None = None
    feature_schema_version: str | None = None
    loaded_at: datetime | None = None


class PredictionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    feature_snapshot_id: str = Field(min_length=1, max_length=100)
    feature_schema_version: str = Field(min_length=1, max_length=100)
    features: dict[str, float] = Field(min_length=1)


class PredictionResponse(BaseModel):
    feature_snapshot_id: str
    model_version: str
    anomaly_score: float = Field(ge=0, le=1)
    is_anomaly: bool
    threshold: float = Field(ge=0, le=1)
    latency_ms: float = Field(ge=0)


class BatchPredictionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    items: list[PredictionRequest] = Field(min_length=1, max_length=1000)
