
import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from datetime import datetime

from features import make_features
from train import PROJECT_DATA, main as train_model

MODEL_PATH = "models/demand_model.joblib"

app = FastAPI(title="Ride Demand ML Service")

try:
    art = joblib.load(MODEL_PATH)
except FileNotFoundError:
    art = None  

class Item(BaseModel):
    area: str
    vehicle: str
    hour: int
    day_of_week: int  # Monday = 0
    month: int


class PredictRequest(BaseModel):
    items: list[Item]


class TrainingSample(BaseModel):
    area: str
    vehicleType: str
    observedAt: datetime
    customers: int


class TrainRequest(BaseModel):
    samples: list[TrainingSample]


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": art is not None}


@app.post("/predict")
def predict(req: PredictRequest):
    if art is None:
        raise HTTPException(status_code=503, detail="Model not found. Run train.py first.")
    if not req.items:
        return {"predictions": []}

    df = pd.DataFrame([item.model_dump() for item in req.items])
    X = make_features(df, art)

    customers = art["reg"].predict(X)
    if art["clf"] is not None:
        probs = art["clf"].predict_proba(X)[:, 1]
    else:
        probs = [0.5] * len(X)

    return {
        "predictions": [
            {
                "expected_customers": round(max(0.0, float(r)), 3),
                "customer_probability": round(float(p), 3),
            }
            for r, p in zip(customers, probs)
        ]
    }


@app.post("/train")
def train(req: TrainRequest):
    global art
    art = train_model(
        PROJECT_DATA,
        MODEL_PATH,
        additional_samples=[sample.model_dump() for sample in req.samples],
    )
    return {"status": "trained", "samplesUsed": len(req.samples)}
