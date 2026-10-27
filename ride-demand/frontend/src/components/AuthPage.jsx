import { useState } from "react";
import { loginDriver, registerDriver } from "../api.js";

export default function AuthPage({ onAuthenticated }) {
  const [registering, setRegistering] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    if (registering && password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      const credentials = { username: username.trim(), password };
      if (registering) await registerDriver(credentials);
      const driver = await loginDriver(credentials);
      onAuthenticated(driver);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function switchMode() {
    setRegistering((current) => !current);
    setError("");
    setPassword("");
    setConfirmPassword("");
  }

  return (
    <main className="auth-page">
      <section className="auth-content">
        <h1 className="brand">RideFlow</h1>
        <form className="auth-card" onSubmit={handleSubmit}>
          <label>
            Driver username
            <input
              autoComplete="username"
              required
              maxLength={50}
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              placeholder="Enter username"
            />
          </label>
          <label>
            Driver password
            <input
              type="password"
              autoComplete={registering ? "new-password" : "current-password"}
              required
              minLength={8}
              maxLength={72}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter password"
            />
          </label>
          {registering && (
            <label>
              Confirm password
              <input
                type="password"
                autoComplete="new-password"
                required
                minLength={8}
                maxLength={72}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                placeholder="Re-enter password"
              />
            </label>
          )}
          {error && <p className="error auth-error" role="alert">{error}</p>}
          <button className="primary auth-submit" type="submit" disabled={loading}>
            {loading ? "Please wait..." : registering ? "Register" : "Login"}
          </button>
          <button className="auth-switch" type="button" onClick={switchMode}>
            {registering ? "Already registered? Login here." : "New user? Register here."}
          </button>
        </form>
      </section>
    </main>
  );
}
