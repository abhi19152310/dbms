
import argparse
import os
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import average_precision_score, mean_absolute_error, mean_squared_error, roc_auc_score
from xgboost import XGBClassifier, XGBRegressor

from features import make_features


PROJECT_DATA = Path.home() / "Downloads" / "rideBookings (1).csv"


def load_rides(path):
    df = pd.read_csv(path, dtype={"Customer ID": "string"})
    required = {"Date", "Time", "Customer ID", "Pickup Location", "Vehicle Type"}
    missing = required.difference(df.columns)
    if missing:
        raise ValueError(f"CSV is missing required columns: {', '.join(sorted(missing))}")

    # build one datetime from the Date and Time columns
    df["datetime"] = pd.to_datetime(
        df["Date"].astype(str) + " " + df["Time"].astype(str),
        format="%d-%m-%Y %H:%M:%S",
        errors="coerce",
    )
    df["Customer ID"] = df["Customer ID"].str.replace('"', "", regex=False).str.strip()
    # drop rows we can't use
    df = df.dropna(subset=["datetime", "Pickup Location", "Vehicle Type", "Customer ID"])
    df["Pickup Location"] = df["Pickup Location"].str.strip()
    df["Vehicle Type"] = df["Vehicle Type"].str.strip()
    df["Customer ID"] = df["Customer ID"].str.strip()
    #Keep the rows only where the pickup location, vehicle type and customer id are not empty
    df = df[
        df["Pickup Location"].ne("")
        & df["Vehicle Type"].ne("")
        & df["Customer ID"].ne("")
    ].copy()
    return df


def count_customers(df):
    """Unique customers per bucket plus weighted samples of zero-customer buckets."""
    df["date"] = df["datetime"].dt.normalize()
    df["hour"] = df["datetime"].dt.hour

    active = (
        df.groupby(["Pickup Location", "Vehicle Type", "date", "hour"])["Customer ID"]
        .nunique()
        .reset_index(name="customers")
        .rename(columns={"Pickup Location": "area", "Vehicle Type": "vehicle"})
    )
    active["day_of_week"] = active["date"].dt.dayofweek  # Monday = 0
    active["month"] = active["date"].dt.month
    active["sample_weight"] = 1.0

    all_areas = np.asarray(sorted(df["Pickup Location"].unique()))
    all_vehicles = sorted(df["Vehicle Type"].unique())
    occupied_by_slot = {
        (pd.Timestamp(date), int(hour), vehicle): group["area"].to_numpy()
        for (date, hour, vehicle), group in active.groupby(["date", "hour", "vehicle"], sort=False)
    }
    random = np.random.default_rng(42)
    zero_rows = []

    # Sample three empty areas per active area/time slot; weight each sample to
    # represent all omitted empty areas during training and evaluation.
    for date in sorted(pd.Timestamp(value) for value in df["date"].unique()):
        for hour in range(24):
            for vehicle in all_vehicles:
                occupied = occupied_by_slot.get((date, hour, vehicle), np.empty(0, dtype=object))
                available = np.setdiff1d(all_areas, occupied, assume_unique=True)
                if not len(available):
                    continue
                sample_count = min(len(available), 3 * max(len(occupied), 1))
                selected = random.choice(available, size=sample_count, replace=False)
                sample_weight = len(available) / sample_count
                day_of_week = date.dayofweek
                month = date.month
                zero_rows.extend(
                    (area, vehicle, date, hour, 0, day_of_week, month, sample_weight)
                    for area in selected
                )

    columns = ["area", "vehicle", "date", "hour", "customers", "day_of_week", "month", "sample_weight"]
    zeros = pd.DataFrame.from_records(zero_rows, columns=columns)
    return pd.concat([active, zeros], ignore_index=True)


def main(data_path, model_path, head=None, additional_samples=None):
    bookings = load_rides(data_path)
    if head is not None:
        bookings = bookings.head(head).copy()
        print(f"Training on first {len(bookings)} rows from {data_path}")
    data = count_customers(bookings)
    print(f"{len(bookings)} bookings -> {len(data)} area/hour customer-count rows")

    # test set = the latest 20% of dates
    all_dates = np.sort(data["date"].unique())
    cutoff = all_dates[int(len(all_dates) * 0.8)]
    train = data[data["date"] < cutoff].copy()
    test = data[data["date"] >= cutoff].copy()

    if additional_samples:
        captured = pd.DataFrame.from_records(additional_samples)
        captured["observedAt"] = pd.to_datetime(captured["observedAt"], errors="coerce")
        captured["customers"] = pd.to_numeric(captured["customers"], errors="raise")
        captured = captured.dropna(subset=["observedAt", "area", "vehicleType"])
        captured = captured[
            captured["area"].astype(str).str.strip().ne("")
            & captured["vehicleType"].astype(str).str.strip().ne("")
            & captured["customers"].ge(0)
        ].copy()
        captured["date"] = captured["observedAt"].dt.normalize()
        captured["hour"] = captured["observedAt"].dt.hour
        captured["day_of_week"] = captured["observedAt"].dt.dayofweek
        captured["month"] = captured["observedAt"].dt.month
        captured = captured.rename(columns={"vehicleType": "vehicle"})
        captured["sample_weight"] = 1.0
        captured = captured[
            ["area", "vehicle", "date", "hour", "customers", "day_of_week", "month", "sample_weight"]
        ]
        train = pd.concat([train, captured], ignore_index=True)
        print(f"Added {len(captured)} captured app observations to training")

    # lookups the model needs later, learned from training data only
    training_dates = train["date"].nunique()
    positive_train = train[train["customers"] > 0]
    past_demand = (
        positive_train.groupby(["area", "vehicle", "hour"])["customers"].sum() / training_dates
    ).to_dict()
    areas = set(data["area"]) | set(train["area"])
    vehicles = set(data["vehicle"]) | set(train["vehicle"])
    total_buckets = training_dates * len(areas) * len(vehicles) * 24
    art = {
        "area_codes": {
            a: i for i, a in enumerate(sorted(areas))
        },
        "vehicle_codes": {
            v: i for i, v in enumerate(sorted(vehicles))
        },
        "past_demand": past_demand,
        "global_mean": float(positive_train["customers"].sum() / total_buckets),
    }

    X_train, X_test = make_features(train, art), make_features(test, art)
    y_train, y_test = train["customers"], test["customers"]

    y_train_high = (y_train > 0).astype(int)
    y_test_high = (y_test > 0).astype(int)

    settings = dict(n_estimators=200, max_depth=5, learning_rate=0.1, random_state=42, n_jobs=-1)
    sample_weights = train["sample_weight"].to_numpy()
    test_weights = test["sample_weight"].to_numpy()
    reg = XGBRegressor(**settings).fit(X_train, y_train, sample_weight=sample_weights)
    clf = None
    if y_train_high.nunique() > 1:
        clf = XGBClassifier(**settings).fit(X_train, y_train_high, sample_weight=sample_weights)

    # ---- evaluation ----
    pred = reg.predict(X_test)
    baseline = X_test["past_demand"]  # "just use the historical average"
    print("\nExpected customers")
    print(f"  MAE      model {mean_absolute_error(y_test, pred, sample_weight=test_weights):.3f}   baseline {mean_absolute_error(y_test, baseline, sample_weight=test_weights):.3f}")
    print(f"  RMSE     model {np.sqrt(mean_squared_error(y_test, pred, sample_weight=test_weights)):.3f}   baseline {np.sqrt(mean_squared_error(y_test, baseline, sample_weight=test_weights)):.3f}")

    if clf is not None:
        prob = clf.predict_proba(X_test)[:, 1]
        print("Customer-presence ranking")
        print(f"  PR-AUC   {average_precision_score(y_test_high, prob, sample_weight=test_weights):.3f}")
        print(f"  ROC-AUC  {roc_auc_score(y_test_high, prob, sample_weight=test_weights):.3f}")
    else:
        prob = np.full(len(X_test), 0.5)
        print("Customer-presence ranking")
        print("  PR-AUC   skipped (only one customer-presence class in training data)")
        print("  ROC-AUC  skipped (only one demand class in training data)")

    art["reg"], art["clf"] = reg, clf
    os.makedirs(os.path.dirname(model_path), exist_ok=True)
    joblib.dump(art, model_path)
    print(f"\nSaved model to {model_path}")
    return art


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=PROJECT_DATA)
    parser.add_argument("--model", default="models/demand_model.joblib")
    parser.add_argument("--head", type=int, default=None, help="Use only the first N rows for a quick training run.")
    args = parser.parse_args()
    main(args.data, args.model, args.head)
