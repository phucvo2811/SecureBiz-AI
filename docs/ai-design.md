# AI/ML Design

## Research question and separation of concerns

The research evaluates whether user behavior aggregated from structured SecureBiz AI activity can identify behavior that differs from an appropriate individual, role, or department baseline.

```text
Observed activity → Feature snapshot → ML anomaly score
ML output + contextual signals → Versioned risk score
Evidence bundle → LLM explanation for an investigator
```

An anomaly is not a claim of fraud, compromise, or malicious intent.

## Data provenance

Every dataset manifest labels records as `SYSTEM_GENERATED`, `SYNTHETIC`, or `PUBLIC`, with source, license/consent basis, extraction query/version, time range, schema version, row count, checksum and exclusions. Synthetic data never masquerades as real operational data. Public datasets are integrated only when licensing and semantic mapping are documented.

## Pipeline

1. Extract append-only activity and necessary business context through a controlled read/export contract.
2. Redact or pseudonymize identifiers for research datasets.
3. Validate schema, missingness, duplication, impossible values and time ordering.
4. Aggregate per subject/window (for example session, hour, or day) without using future information.
5. Derive versioned features and fit preprocessing only on the training split.
6. Train candidate Isolation Forest and One-Class SVM; add an autoencoder only when sample size and justification support it.
7. Calibrate decision threshold using validation data/operational false-positive budget, persist full artifact metadata, then evaluate once on held-out data.
8. Load the selected persisted pipeline at service startup; never train during prediction.

## Initial feature schema

Authentication: login-hour cyclical encoding, failed-login count, new device/IP relative to subject history, session duration.

Behavior: action volume/rate, unique modules, rarity relative to baseline, denied and sensitive actions.

Inventory: adjustment count/quantity, outbound quantity/value, unique variants modified, deviation from role baseline.

Sales: orders, cancellations, refunds, discounts, average discount and sales value.

Documents: views/downloads, sensitive accesses and volume. Features must declare window, units, missing-value behavior, allowed range and leakage risk.

## Baselines

The implemented contextual risk layer uses a 30-day hierarchical fallback: individual baseline at five prior system snapshots; otherwise an active-role cohort at ten; then department cohort at ten; then global history; otherwise `NONE`. The selected scope, sample size and mean vector are persisted with every risk assessment and returned to Security Center. The ML artifact remains independently versioned; future experiments may compare baseline-normalized model inputs rather than assuming they improve detection.

## Experiment design

- Split chronologically and, where relevant, group by user/session to prevent leakage.
- Fix random seeds and record library versions, code commit, configuration, feature schema and dataset checksum.
- Never tune on the final test set.
- Evaluate labeled data using precision, recall, F1, false-positive rate, PR-AUC and ROC-AUC where class distribution makes it meaningful.
- Without credible labels, report contamination/sensitivity analysis, score distributions, stability, investigator feedback and explicitly limited conclusions.
- Report inference latency and operational alert volume; model quality alone is insufficient.

Synthetic anomalies are useful for controlled coverage but cannot establish real-world detection performance by themselves. Labels derived from the same simple rules being evaluated must be disclosed to avoid circular validation.

## Artifact contract

A deployable version contains preprocessing and estimator artifacts, feature order/schema, algorithm/hyperparameters, threshold, expected library versions, training manifest checksum, evaluation metrics, artifact checksum and lifecycle status (`CANDIDATE`, `STAGING`, `ACTIVE`, `RETIRED`). Only one active version exists per detection task/environment.

Prediction output includes model/version, feature snapshot ID, raw decision score, normalized anomaly score with documented direction, threshold, anomaly flag, baseline type, latency and warnings. Normalization is learned/documented per model—not an arbitrary random or hard-coded number.

## Risk engine

The backend owns a versioned, explainable policy. A starting research formulation may combine normalized components such as anomaly evidence, action/resource sensitivity, business impact, denied-access signals and contextual novelty. Weights and severity thresholds are stored in `risk_policy_versions`, sum/normalization is validated, and each assessment stores component contributions and reason codes. Rule signals remain clearly labeled as rules, not ML.

## Monitoring and feedback

Monitor input missingness/range, baseline coverage, feature/score drift, latency, failure rate, alert volume and investigator dispositions. `FALSE_POSITIVE` is feedback, not automatically a training label; reviewed feedback enters a curated, versioned labeling process.

## LLM copilot

After the security core is stable, backend retrieval builds a bounded evidence packet containing alert, risk contributions, relevant activity timeline and model/policy metadata. The prompt requires evidence references and forbids invented facts. Provider/model/prompt version, evidence IDs, response and reviewer feedback are logged with sensitive-data controls. The LLM never determines the anomaly score and never becomes the authoritative risk engine.
