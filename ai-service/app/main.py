from fastapi import Depends, FastAPI, HTTPException, status
from time import perf_counter
import numpy as np

from app.config import get_settings
from app.model_registry import ModelRegistry
from app.schemas import BatchPredictionRequest, HealthResponse, ModelInfoResponse, PredictionRequest, PredictionResponse
from app.security import require_internal_api_key

settings = get_settings()
registry = ModelRegistry(settings.model_dir)
registry.load_active()

app = FastAPI(
    title="SecureBiz AI Detection Service",
    version="0.1.0",
    description="Internal, versioned anomaly-detection inference API",
)


@app.get("/health", response_model=HealthResponse, tags=["operations"])
def health() -> HealthResponse:
    return HealthResponse(status="ok", service=settings.service_name)


@app.get("/model/info", response_model=ModelInfoResponse, tags=["model"], dependencies=[Depends(require_internal_api_key)])
def model_info() -> ModelInfoResponse:
    loaded = registry.loaded
    if loaded is None:
        metadata = registry.read_active_metadata()
        if metadata is None:
            return ModelInfoResponse(available=False)
        return ModelInfoResponse(
            available=False,
            model_version=metadata.get("model_version"),
            algorithm=metadata.get("algorithm"),
            feature_schema_version=metadata.get("feature_schema_version"),
        )
    return ModelInfoResponse(
        available=True,
        model_version=loaded.metadata.get("model_version"),
        algorithm=loaded.metadata.get("algorithm"),
        feature_schema_version=loaded.metadata.get("feature_schema_version"),
        loaded_at=loaded.loaded_at,
    )


@app.post("/predict", response_model=PredictionResponse, tags=["prediction"], dependencies=[Depends(require_internal_api_key)])
def predict(request: PredictionRequest) -> PredictionResponse:
    loaded=registry.loaded
    if loaded is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,detail="No validated model artifact is active")
    expected=loaded.metadata.get("feature_schema_version")
    if request.feature_schema_version != expected:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,detail="Feature schema is incompatible with the active model")
    names=loaded.metadata.get("feature_names",[]);missing=set(names)-set(request.features);extra=set(request.features)-set(names)
    if missing or extra:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,detail={"missing_features":sorted(missing),"unexpected_features":sorted(extra)})
    started=perf_counter();values=np.asarray([[request.features[name] for name in names]],dtype=float);score=float(loaded.estimator.score(values)[0]);latency=(perf_counter()-started)*1000
    threshold=float(loaded.metadata["threshold"])
    return PredictionResponse(feature_snapshot_id=request.feature_snapshot_id,model_version=str(loaded.metadata["model_version"]),anomaly_score=score,is_anomaly=score>=threshold,threshold=threshold,latency_ms=latency)


@app.post("/batch-predict", response_model=list[PredictionResponse], tags=["prediction"], dependencies=[Depends(require_internal_api_key)])
def batch_predict(request: BatchPredictionRequest) -> list[PredictionResponse]:
    return [predict(item) for item in request.items]
