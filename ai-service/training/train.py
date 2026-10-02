from __future__ import annotations
import argparse, hashlib, json
from datetime import UTC, datetime
from pathlib import Path
import joblib, numpy as np, pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.svm import OneClassSVM
from evaluation.metrics import evaluate
from features.schema import FEATURE_NAMES, FEATURE_SCHEMA_VERSION
from models.bundle import ModelBundle
from preprocessing.pipeline import build_preprocessor, validate_matrix


def train_candidate(name: str, train: np.ndarray, contamination: float, seed: int) -> ModelBundle:
    prep=build_preprocessor(); transformed=prep.fit_transform(train)
    estimator=IsolationForest(n_estimators=300,contamination=contamination,random_state=seed,n_jobs=-1) if name=="isolation_forest" else OneClassSVM(kernel="rbf",gamma="scale",nu=contamination)
    estimator.fit(transformed); raw=-estimator.decision_function(transformed);low,high=np.quantile(raw,[0.01,0.99]);scores=np.clip((raw-low)/max(high-low,1e-12),0,1);threshold=float(np.quantile(scores,1-contamination))
    return ModelBundle(prep,estimator,FEATURE_NAMES,threshold,float(low),float(high))


def main() -> None:
    parser=argparse.ArgumentParser();parser.add_argument("--dataset",type=Path,required=True);parser.add_argument("--output",type=Path,required=True);parser.add_argument("--algorithm",choices=["isolation_forest","one_class_svm"],default="isolation_forest");parser.add_argument("--contamination",type=float,default=.03);parser.add_argument("--seed",type=int,default=42);parser.add_argument("--activate",action="store_true");args=parser.parse_args()
    if not 0<args.contamination<.5: raise ValueError("contamination must be between 0 and 0.5")
    data=pd.read_csv(args.dataset);missing=set(FEATURE_NAMES)-set(data.columns)
    if missing: raise ValueError(f"Missing features: {sorted(missing)}")
    if "window_start" in data.columns:
        data=data.assign(window_start=pd.to_datetime(data["window_start"],utc=True,errors="raise")).sort_values("window_start").reset_index(drop=True)
    values=data[FEATURE_NAMES].to_numpy(dtype=float);validate_matrix(values);split=max(int(len(values)*.8),20);train,test=values[:split],values[split:] if split<len(values) else values[-min(20,len(values)):]
    bundle=train_candidate(args.algorithm,train,args.contamination,args.seed);scores=bundle.score(test);labels=data["is_anomaly"].to_numpy(dtype=int)[-len(test):] if "is_anomaly" in data else None
    metrics=evaluate(scores,bundle.threshold,labels,bundle.score,test);version=datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")+"-"+args.algorithm;args.output.mkdir(parents=True,exist_ok=True);artifact=args.output/f"{version}.joblib";joblib.dump(bundle,artifact);digest=hashlib.sha256(artifact.read_bytes()).hexdigest()
    metadata={"model_version":version,"algorithm":args.algorithm,"feature_schema_version":FEATURE_SCHEMA_VERSION,"feature_names":FEATURE_NAMES,"threshold":bundle.threshold,"trained_at":datetime.now(UTC).isoformat(),"random_seed":args.seed,"contamination":args.contamination,"dataset_path":str(args.dataset),"dataset_rows":len(data),"chronological_split":split,"metrics":metrics,"artifact":artifact.name,"sha256":digest,"labels_available":labels is not None}
    (args.output/f"{version}.json").write_text(json.dumps(metadata,indent=2),encoding="utf-8")
    if args.activate:(args.output/"active-model.json").write_text(json.dumps(metadata,indent=2),encoding="utf-8")
    print(json.dumps(metadata,indent=2))

if __name__=="__main__":main()
