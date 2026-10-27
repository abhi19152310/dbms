
import pandas as pd

FEATURES = ["area_code", "vehicle_code", "hour", "day_of_week", "month", "past_demand"]


def make_features(df, art):
    """
    df needs these columns: area, vehicle, hour, day_of_week, month
    art is the dict of saved lookups (area codes, past demand, etc.)
    """
    out = pd.DataFrame(index=df.index)

    # unknown areas / vehicles become -1 instead of crashing
    out["area_code"] = df["area"].map(art["area_codes"]).fillna(-1).astype(int)
    out["vehicle_code"] = df["vehicle"].map(art["vehicle_codes"]).fillna(-1).astype(int)

    out["hour"] = df["hour"]
    out["day_of_week"] = df["day_of_week"]
    out["month"] = df["month"]

    # average customers seen before for this area + vehicle + hour
    keys = zip(df["area"], df["vehicle"], df["hour"])
    out["past_demand"] = [art["past_demand"].get(k, art["global_mean"]) for k in keys]

    return out[FEATURES]
