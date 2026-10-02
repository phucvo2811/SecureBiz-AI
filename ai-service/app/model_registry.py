import json
import hashlib
import joblib
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class LoadedModel:
    estimator: Any
    metadata: dict[str, Any]
    loaded_at: datetime


class ModelRegistry:
    """Loads only an explicitly activated, versioned artifact.

    Artifacts are loaded only when the active metadata and SHA-256 digest match.
    """

    def __init__(self, model_dir: Path) -> None:
        self._model_dir = model_dir
        self._loaded: LoadedModel | None = None

    @property
    def loaded(self) -> LoadedModel | None:
        return self._loaded

    def read_active_metadata(self) -> dict[str, Any] | None:
        path = self._model_dir / "active-model.json"
        if not path.is_file():
            return None
        return json.loads(path.read_text(encoding="utf-8"))

    def load_active(self) -> LoadedModel | None:
        metadata = self.read_active_metadata()
        if metadata is None:
            self._loaded = None
            return None
        artifact = self._model_dir / str(metadata["artifact"])
        if not artifact.is_file():
            self._loaded = None
            return None
        digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
        if digest != metadata.get("sha256"):
            raise ValueError("Active model artifact digest mismatch")
        self._loaded = LoadedModel(joblib.load(artifact), metadata, datetime.now(UTC))
        return self._loaded

    def mark_metadata_loaded(self, metadata: dict[str, Any]) -> None:
        self._loaded = LoadedModel(estimator=None, metadata=metadata, loaded_at=datetime.now(UTC))
