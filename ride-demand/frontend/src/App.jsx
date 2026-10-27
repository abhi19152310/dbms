import { useEffect, useState } from "react";
import { getCurrentDriver, logoutDriver, predictDemand } from "./api.js";
import AuthPage from "./components/AuthPage.jsx";
import SearchPanel from "./components/SearchPanel.jsx";
import ResultsList from "./components/ResultsList.jsx";
import UnmetDemandForm from "./components/UnmetDemandForm.jsx";
import DemandMap from "./components/DemandMap.jsx";
import RideFoundButton from "./components/RideFoundButton.jsx";

export default function App() {
  const [driver, setDriver] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [sessionError, setSessionError] = useState("");

  useEffect(() => {
    let active = true;
    getCurrentDriver()
      .then((currentDriver) => {
        if (active) setDriver(currentDriver);
      })
      .catch((err) => {
        if (active) setSessionError(err.message);
      })
      .finally(() => {
        if (active) setCheckingSession(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function handleLogout() {
    try {
      await logoutDriver();
      setDriver(null);
    } catch (err) {
      setSessionError(err.message);
    }
  }

  if (checkingSession) return <main className="auth-page"><p>Loading RideFlow...</p></main>;
  if (!driver) {
    return (
      <>
        {sessionError && <p className="error session-error" role="alert">{sessionError}</p>}
        <AuthPage onAuthenticated={setDriver} />
      </>
    );
  }

  return <Dashboard driver={driver} onLogout={handleLogout} />;
}

function Dashboard({ driver, onLogout }) {
  // starting values: Gurugram, evening rush
  const [search, setSearch] = useState({
    latitude: 28.4593,
    longitude: 77.0727,
    vehicle: "Auto",
    time: "19:00",
  });
  const [areas, setAreas] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);

  async function handlePredict() {
    setLoading(true);
    setError("");
    try {
      const data = await predictDemand(search);
      setAreas(data.areas);
      setSearched(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="driver-session">
          <span>Signed in as <strong>{driver.username}</strong></span>
          <button type="button" onClick={onLogout}>Log out</button>
        </div>
        <h1>Where should I go next?</h1>
        <p className="subtitle">Estimate customers by nearby pickup area, vehicle and time.</p>

        <SearchPanel search={search} onChange={setSearch} onPredict={handlePredict} loading={loading} />

        {error && <p className="error">{error}</p>}

        <ResultsList areas={areas} searched={searched} />

        <RideFoundButton vehicleType={search.vehicle} />

        <UnmetDemandForm username={driver.username} vehicle={search.vehicle} />
      </aside>

      <main className="map-area">
        <DemandMap driver={search} areas={areas} />
      </main>
    </div>
  );
}
