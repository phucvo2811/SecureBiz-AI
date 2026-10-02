from __future__ import annotations

import pandas as pd

from features.schema import FEATURE_NAMES

SENSITIVE_ACTIONS = {"ROLE_CHANGE", "PERMISSION_CHANGE", "INVENTORY_ADJUST", "REFUND", "DOCUMENT_DOWNLOAD"}


def build_user_windows(logs: pd.DataFrame, window: str = "1h") -> pd.DataFrame:
    """Create deterministic user/time-window features from exported structured activity logs."""
    required = {"user_id", "occurred_at", "action", "module", "result"}
    missing = required - set(logs.columns)
    if missing:
        raise ValueError(f"Missing activity columns: {sorted(missing)}")
    frame = logs.copy()
    frame["occurred_at"] = pd.to_datetime(frame["occurred_at"], utc=True, errors="raise")
    frame["window_start"] = frame["occurred_at"].dt.floor(window)
    for column in ["quantity", "discount", "amount", "session_duration_minutes"]:
        if column not in frame:
            frame[column] = 0.0
        frame[column] = pd.to_numeric(frame[column], errors="coerce").fillna(0.0)
    if "device_id_hash" not in frame: frame["device_id_hash"] = ""
    if "ip_address" not in frame: frame["ip_address"] = ""
    if "resource_id" not in frame: frame["resource_id"] = ""
    frame["minute"] = frame["occurred_at"].dt.floor("min")
    frame["login_hour"] = frame["occurred_at"].dt.hour.where(frame["action"].eq("LOGIN_SUCCESS"))
    frame["is_sensitive"] = frame["action"].isin(SENSITIVE_ACTIONS).astype(int)
    frame["is_denied"] = frame["result"].eq("DENIED").astype(int)

    rows: list[dict[str, object]] = []
    for (user_id, start), group in frame.groupby(["user_id", "window_start"], sort=True):
        minutes = max(group["minute"].nunique(), 1)
        action = group["action"]
        inventory = group["module"].eq("inventory")
        sales = group["module"].eq("sales")
        document = group["module"].eq("document")
        row = {
            "user_id": str(user_id), "window_start": start,
            "login_hour_mean": group["login_hour"].mean() if group["login_hour"].notna().any() else 12.0,
            "failed_login_count": action.eq("LOGIN_FAILED").sum(),
            "new_device_count": group.loc[group["device_id_hash"].ne(""), "device_id_hash"].nunique(),
            "new_ip_count": group.loc[group["ip_address"].ne(""), "ip_address"].nunique(),
            "session_duration_minutes": group["session_duration_minutes"].max(),
            "actions_count": len(group), "actions_per_minute": len(group) / minutes,
            "unique_modules": group["module"].nunique(), "rare_module_access": 0.0,
            "denied_access_count": group["is_denied"].sum(), "sensitive_action_count": group["is_sensitive"].sum(),
            "stock_adjustment_count": (action.eq("INVENTORY_ADJUST") & inventory).sum(),
            "stock_adjustment_quantity": group.loc[action.eq("INVENTORY_ADJUST") & inventory, "quantity"].abs().sum(),
            "stock_export_quantity": group.loc[inventory & group["quantity"].lt(0), "quantity"].abs().sum(),
            "unique_products_modified": group.loc[inventory & group["resource_id"].ne(""), "resource_id"].nunique(),
            "orders_count": (action.eq("ORDER_CREATE") & sales).sum(), "cancel_count": action.eq("ORDER_CANCEL").sum(),
            "refund_count": action.isin(["REFUND", "RETURN_APPROVE"]).sum(),
            "discount_count": group["discount"].gt(0).sum(), "average_discount": group["discount"].mean(),
            "sales_value": group.loc[sales, "amount"].sum(), "documents_viewed": action.eq("DOCUMENT_VIEW").sum(),
            "documents_downloaded": action.eq("DOCUMENT_DOWNLOAD").sum(),
            "sensitive_documents_accessed": (document & group["action"].isin(["DOCUMENT_VIEW", "DOCUMENT_DOWNLOAD"]) & group.get("sensitivity", pd.Series("", index=group.index)).isin(["CONFIDENTIAL", "RESTRICTED"])).sum(),
        }
        rows.append(row)
    result = pd.DataFrame(rows)
    if result.empty: return pd.DataFrame(columns=["user_id", "window_start", *FEATURE_NAMES])
    return result[["user_id", "window_start", *FEATURE_NAMES]].fillna(0.0)
