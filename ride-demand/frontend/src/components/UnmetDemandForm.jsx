import { useState } from "react";
import { reportUnmetDemand } from "../api.js";

export default function UnmetDemandForm({ username, vehicle }) {
  const [area, setArea] = useState("");
  const [message, setMessage] = useState("");
  const [failed, setFailed] = useState(false);

  async function handleSubmit() {
    setMessage("");
    try {
      await reportUnmetDemand({ area, vehicleType: vehicle });
      setFailed(false);
      setMessage("Thanks, your report was saved.");
      setArea("");
    } catch (err) {
      setFailed(true);
      setMessage(err.message);
    }
  }

  return (
    <details className="card">
      <summary>Report an unsuccessful search</summary>
      <p className="hint">You can send one report per day. It counts as demand, not as a booking.</p>
      <p className="hint">Report submitted as {username}.</p>
      <label>
        Area
        <input value={area} onChange={(e) => setArea(e.target.value)} placeholder="e.g. Anna Nagar" />
      </label>
      <button type="button" onClick={handleSubmit} disabled={!area}>
        Send report
      </button>
      {message && <p className={failed ? "error" : "success"}>{message}</p>}
    </details>
  );
}
