# Backend (Spring Boot)

Needs Java 17, Maven and PostgreSQL.

1. Create a database: `createdb ride_demand`; set `DB_PASSWORD` to the PostgreSQL password before starting the backend.
2. Start the Python ML service first (see `../ml-service/README.md`)
3. Run: `mvn spring-boot:run`  ->  http://localhost:8080

## Endpoints

Register a driver with `POST /api/auth/register` using `{"username":"...","password":"at-least-8-characters"}`, then sign in with `POST /api/auth/login`. Both calls return a session cookie; send that cookie with subsequent API requests. Passwords are stored as BCrypt hashes. All `/api/**` endpoints other than registration and login require an authenticated driver session.

| Method | Path | What it does |
|---|---|---|
| POST | `/api/auth/register` | creates a driver account |
| POST | `/api/auth/login` | signs in and starts a driver session |
| GET | `/api/auth/me` | returns the signed-in driver |
| POST | `/api/auth/logout` | ends the driver session |
| POST | `/api/demand/predict` | demand for areas near the driver, ranked |
| POST | `/api/demand/predict` | estimates distinct customers by nearby pickup area, vehicle and time |
| POST | `/api/reports/unmet-demand` | reports a failed search for the signed-in driver (once per day) |
| POST | `/api/observations` | driver app sends a location/online snapshot (needs `consent: true`) |
| POST | `/api/observations/ride-found` | captain records a found ride with GPS coordinates (needs `consent: true`) |
| POST | `/api/training/run` | trains on new unmet-demand reports and labeled driver ride outcomes |

Training runs automatically about once a minute when at least 10 new eligible
records are waiting. Each run retrains with the original bookings data and all
eligible rider reports and driver outcomes in the database, so older collected
data stays in the training set. Records are marked as used only after the model
is saved.

An authenticated driver can also call `/api/training/run` to train immediately. It returns
`{"status":"trained","samplesUsed":N}` when the model is rebuilt, or
`{"status":"no_new_samples","samplesUsed":0}` when there is nothing eligible.
Rider reports are positive demand examples. Driver outcomes `FOUND_RIDE` and
`NO_RIDE` are used as positive and zero-customer examples respectively; GPS-only
observations are assigned to their nearest configured area. Failed runs leave
new records unmarked so they can be retried.

Example login flow:

```bash
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"username":"driver1","password":"your-password"}'
curl -c cookies.txt -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"driver1","password":"your-password"}'
curl -b cookies.txt -X POST localhost:8080/api/demand/predict -H "Content-Type: application/json" \
  -d '{"latitude": 28.4593, "longitude": 77.0727, "vehicle": "Auto", "time": "19:00"}'
```

To record a ride found at the captain's current location:

```bash
curl -X POST localhost:8080/api/observations/ride-found -H "Content-Type: application/json" \
  -b cookies.txt -d '{"vehicleType": "Auto", "latitude": 28.4593, "longitude": 77.0727, "consent": true}'
```
