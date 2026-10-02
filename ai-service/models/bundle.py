from dataclasses import dataclass
from typing import Any

@dataclass
class ModelBundle:
    preprocessor: Any
    estimator: Any
    feature_names: list[str]
    threshold: float
    score_min: float
    score_max: float

    def score(self, values: Any) -> Any:
        transformed = self.preprocessor.transform(values)
        raw = -self.estimator.decision_function(transformed)
        scale = max(self.score_max - self.score_min, 1e-12)
        return ((raw - self.score_min) / scale).clip(0.0, 1.0)
