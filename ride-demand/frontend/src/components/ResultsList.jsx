import { demandColor } from "./DemandMap.jsx";

export default function ResultsList({ areas, searched }) {
  if (!searched) return null;

  if (areas.length === 0) {
    return <p className="hint">No known pickup areas within range of this location.</p>;
  }

  return (
    <section className="card">
      <h2>Customer estimates</h2>
      <ol className="results">
        {areas.map((a) => (
          <li key={a.area}>
            <div className="result-top">
              <strong>{a.area}</strong>
              <span>{Math.round(a.customerProbability * 100)}% chance this hour</span>
            </div>
            <div className="bar">
              <div
                className="bar-fill"
                style={{ width: `${a.customerProbability * 100}%`, background: demandColor(a.customerProbability) }}
              />
            </div>
            <div className="result-bottom">
              {Math.round(a.expectedCustomers) > 1 && (
                <>
                  {Math.round(a.expectedCustomers)} expected customers this hour &middot;{" "}
                </>
              )}
              {a.distanceKm} km away
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}
