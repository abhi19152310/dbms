const VEHICLES = ["Auto", "Bike", "eBike", "Go Mini", "Go Sedan", "Premier Sedan", "Uber XL"];

export default function SearchPanel({ search, onChange, onPredict, loading }) {
  // update one field without touching the others
  function update(field, value) {
    onChange({ ...search, [field]: value });
  }

  function useMyLocation() {
    if (!navigator.geolocation) {
      alert("Your browser doesn't support location.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => onChange({ ...search, latitude: pos.coords.latitude, longitude: pos.coords.longitude }),
      () => alert("Couldn't get your location. You can type it in instead.")
    );
  }

  return (
    <section className="card">
      <h2>Current location</h2>
      <div className="row">
        <label>
          Latitude
          <input
            type="number"
            step="0.0001"
            value={search.latitude}
            onChange={(e) => update("latitude", Number(e.target.value))}
          />
        </label>
        <label>
          Longitude
          <input
            type="number"
            step="0.0001"
            value={search.longitude}
            onChange={(e) => update("longitude", Number(e.target.value))}
          />
        </label>
      </div>
      <button type="button" className="link-button" onClick={useMyLocation}>
        Use my GPS location
      </button>

      <div className="row">
        <label>
          Time
          <input type="time" value={search.time} onChange={(e) => update("time", e.target.value)} />
        </label>
        <label>
          Vehicle
          <select value={search.vehicle} onChange={(e) => update("vehicle", e.target.value)}>
            {VEHICLES.map((v) => (
              <option key={v}>{v}</option>
            ))}
          </select>
        </label>
      </div>

      <button type="button" className="primary" onClick={onPredict} disabled={loading}>
        {loading ? "Estimating..." : "Estimate customers"}
      </button>
    </section>
  );
}
