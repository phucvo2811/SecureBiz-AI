from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

from features.schema import FEATURE_NAMES


def generate(rows: int, anomalies: int, seed: int) -> pd.DataFrame:
    """Generate a labeled experimental dataset; never treat it as real user activity."""
    if rows < 100 or anomalies < 1 or anomalies >= rows // 2:
        raise ValueError("Use at least 100 rows and fewer than 50% anomalies")
    rng = np.random.default_rng(seed)
    data = pd.DataFrame(0.0, index=np.arange(rows), columns=FEATURE_NAMES)
    normal = rows - anomalies
    data.loc[: normal - 1, "login_hour_mean"] = rng.normal(10, 1.8, normal).clip(0, 23)
    data.loc[: normal - 1, "actions_count"] = rng.poisson(20, normal)
    data.loc[: normal - 1, "actions_per_minute"] = rng.gamma(2, .18, normal)
    data.loc[: normal - 1, "unique_modules"] = rng.integers(1, 4, normal)
    data.loc[: normal - 1, "failed_login_count"] = rng.binomial(2, .05, normal)
    data.loc[: normal - 1, "denied_access_count"] = rng.binomial(2, .03, normal)
    data.loc[: normal - 1, "orders_count"] = rng.poisson(3, normal)
    data.loc[: normal - 1, "sales_value"] = rng.gamma(2, 800_000, normal)
    data.loc[: normal - 1, "documents_viewed"] = rng.poisson(1, normal)
    data.loc[: normal - 1, "session_duration_minutes"] = rng.gamma(3, 15, normal)
    data.loc[: normal - 1, "new_ip_count"] = 1
    data.loc[: normal - 1, "new_device_count"] = 1

    idx = data.index[normal:]
    data.loc[idx, "login_hour_mean"] = rng.choice([1, 2, 23], anomalies)
    data.loc[idx, "actions_count"] = rng.integers(120, 300, anomalies)
    data.loc[idx, "actions_per_minute"] = rng.uniform(5, 15, anomalies)
    data.loc[idx, "unique_modules"] = rng.integers(5, 9, anomalies)
    data.loc[idx, "rare_module_access"] = rng.integers(2, 8, anomalies)
    data.loc[idx, "failed_login_count"] = rng.integers(4, 15, anomalies)
    data.loc[idx, "denied_access_count"] = rng.integers(5, 20, anomalies)
    data.loc[idx, "sensitive_action_count"] = rng.integers(5, 25, anomalies)
    data.loc[idx, "stock_adjustment_count"] = rng.integers(3, 15, anomalies)
    data.loc[idx, "stock_adjustment_quantity"] = rng.uniform(100, 1000, anomalies)
    data.loc[idx, "stock_export_quantity"] = rng.uniform(100, 1500, anomalies)
    data.loc[idx, "refund_count"] = rng.integers(2, 12, anomalies)
    data.loc[idx, "documents_downloaded"] = rng.integers(10, 80, anomalies)
    data.loc[idx, "sensitive_documents_accessed"] = rng.integers(3, 30, anomalies)
    data["is_anomaly"] = 0
    data.loc[idx, "is_anomaly"] = 1
    data = data.sample(frac=1, random_state=seed).reset_index(drop=True)
    data.insert(0, "window_start", pd.date_range("2026-01-01", periods=rows, freq="h", tz="UTC"))
    return data


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--rows", type=int, default=500)
    parser.add_argument("--anomalies", type=int, default=25)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    generate(args.rows, args.anomalies, args.seed).to_csv(args.output, index=False)
    print(f"wrote synthetic dataset: {args.output} ({args.rows} rows, {args.anomalies} anomalies)")


if __name__ == "__main__":
    main()
