from __future__ import annotations
from time import perf_counter
from typing import Any
import numpy as np
from sklearn.metrics import average_precision_score, confusion_matrix, f1_score, precision_score, recall_score, roc_auc_score


def evaluate(scores: np.ndarray, threshold: float, labels: np.ndarray | None, predict_fn: Any, sample: np.ndarray) -> dict[str, float | None]:
    started = perf_counter(); predict_fn(sample); latency = (perf_counter() - started) * 1000 / max(len(sample), 1)
    result: dict[str, float | None] = {"prediction_latency_ms_per_row": latency, "score_mean": float(scores.mean()), "score_std": float(scores.std())}
    if labels is None:
        result.update({"precision": None, "recall": None, "f1": None, "false_positive_rate": None, "pr_auc": None, "roc_auc": None})
        return result
    predicted = scores >= threshold
    result.update({"precision": float(precision_score(labels,predicted,zero_division=0)),"recall":float(recall_score(labels,predicted,zero_division=0)),"f1":float(f1_score(labels,predicted,zero_division=0)),"pr_auc":float(average_precision_score(labels,scores)),"roc_auc":float(roc_auc_score(labels,scores)) if len(np.unique(labels))>1 else None})
    matrix=confusion_matrix(labels,predicted,labels=[0,1]);tn,fp=matrix[0];result["false_positive_rate"]=float(fp/(fp+tn)) if fp+tn else 0.0
    return result
