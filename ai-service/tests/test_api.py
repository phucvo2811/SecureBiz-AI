import os
import tempfile

from fastapi.testclient import TestClient

os.environ["AI_MODEL_DIR"] = tempfile.mkdtemp(prefix="securebiz-empty-model-")

from app.main import app

client = TestClient(app)
SERVICE_HEADERS = {"X-Internal-Api-Key": "replace-with-random-service-credential"}


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_predict_does_not_fabricate_score_without_model() -> None:
    response = client.post(
        "/predict",
        json={
            "feature_snapshot_id": "snapshot-1",
            "feature_schema_version": "v1",
            "features": {"actions_count": 2.0},
        },
        headers=SERVICE_HEADERS,
    )
    assert response.status_code == 503
    assert response.json()["detail"] == "No validated model artifact is active"


def test_predict_rejects_missing_service_credential() -> None:
    response = client.post(
        "/predict",
        json={
            "feature_snapshot_id": "snapshot-1",
            "feature_schema_version": "v1",
            "features": {"actions_count": 2.0},
        },
    )
    assert response.status_code == 401
