# ML service (Python)

Install the dependencies once:

```powershell
python -m pip install -r requirements.txt
```

The default training file is `~/Downloads/rideBookings (1).csv`. It needs `Date`, `Time`, `Customer ID`, `Vehicle Type`, and `Pickup Location` columns.

```powershell
python train.py
```

Training groups bookings by pickup area, vehicle, date, and hour, and uses the number of distinct customer IDs as the target. It samples zero-customer area/time buckets so the model learns both customer counts and the chance of any customers, weighting samples to represent omitted empty buckets. The model evaluates on the latest dates and saves `models/demand_model.joblib`. All booking statuses are included because unfulfilled searches still represent customer demand. To use another CSV, run `python train.py --data path\to\bookings.csv`.

On the attached 2024 CSV, the chronological holdout has customer-count MAE 0.028 and customer-presence ROC-AUC 0.689. Counts are sparse at the area/vehicle/hour level, so interpret them as expected values rather than guaranteed whole customers.

The backend can retrain the model with newly collected rider reports and labeled
driver ride outcomes by calling `POST /api/training/run`. The Python service's
`POST /train` accepts these samples and adds them to the training split using
the same area, vehicle, hour, weekday, month, and historical-demand features as
the booking data. The backend marks the included records as used only after
training and model persistence succeed.

The NCR seed coordinates in the backend SQL are sourced from OpenStreetMap: © OpenStreetMap contributors, ODbL 1.0.

Run the prediction API after training:

```powershell
python -m uvicorn predict_service:app --port 8000
```

`GET /health` reports whether the customer-demand model loaded. Predictions use `POST /predict`.
