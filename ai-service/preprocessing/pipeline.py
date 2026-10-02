from __future__ import annotations
import numpy as np
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import RobustScaler


def build_preprocessor() -> Pipeline:
    return Pipeline([("imputer", SimpleImputer(strategy="median")), ("scaler", RobustScaler())])


def validate_matrix(values: np.ndarray) -> None:
    if values.ndim != 2 or values.shape[0] < 20:
        raise ValueError("Training requires at least 20 feature rows")
    if not np.isfinite(values).all():
        raise ValueError("Feature matrix contains non-finite values")
