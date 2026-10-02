# Reproducible AI experiments

SecureBiz separates feature extraction, anomaly detection, risk assessment and explanation. Model output is an anomaly score—not a verdict of fraud or malicious intent.

## Dataset contract

Export structured activity rows with a documented origin (`system`, `synthetic`, or an identified public dataset). `features.activity_features.build_user_windows` creates hourly user windows under schema `behavior-v1`. Sort feature snapshots chronologically before training; the trainer uses the oldest 80% for fitting and the newest 20% for evaluation to reduce temporal leakage.

Synthetic data must be stored and reported separately from system-generated observations. Never copy synthetic evaluation metrics into thesis results as production performance.

## Training

```bash
python -m training.train --dataset datasets/features.csv --output models/artifacts --algorithm isolation_forest --activate
python -m training.train --dataset datasets/features.csv --output models/candidates --algorithm one_class_svm
```

The run fixes the configured random seed, persists the preprocessing pipeline with the estimator, hashes the artifact, records feature schema/version and writes raw metrics. When `is_anomaly` ground truth is absent, precision, recall, F1, FPR, PR-AUC and ROC-AUC remain `null`; only latency and score-distribution diagnostics are emitted. This deliberately avoids fabricated effectiveness claims.

Activation is explicit through `active-model.json`. The inference service verifies the artifact SHA-256 and rejects incompatible or incomplete feature schemas.

An autoencoder is intentionally deferred until the dataset is sufficiently large to justify a neural baseline and a stable validation split.
