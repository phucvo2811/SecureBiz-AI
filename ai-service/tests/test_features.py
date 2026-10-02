import pandas as pd
from features.activity_features import build_user_windows
from features.schema import FEATURE_NAMES


def test_feature_windows_are_deterministic_and_schema_complete() -> None:
    logs = pd.DataFrame([
        {"user_id":"u1","occurred_at":"2026-01-01T10:00:00Z","action":"LOGIN_SUCCESS","module":"identity","result":"SUCCESS","ip_address":"10.0.0.1","device_id_hash":"d1"},
        {"user_id":"u1","occurred_at":"2026-01-01T10:01:00Z","action":"ACCESS_DENIED","module":"document","result":"DENIED","ip_address":"10.0.0.1","device_id_hash":"d1"},
    ])
    result=build_user_windows(logs)
    assert list(result.columns)==["user_id","window_start",*FEATURE_NAMES]
    assert result.iloc[0]["actions_count"]==2
    assert result.iloc[0]["denied_access_count"]==1
