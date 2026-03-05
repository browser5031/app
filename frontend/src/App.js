import { useState, useEffect, useCallback, useRef } from "react";
import "@/App.css";
import axios from "axios";
import { ShieldCheck, ShieldAlert, Radio, Activity, Lock, Unlock, Settings, List, Signal, Wifi, MapPin, Trash2, Power, PowerOff, ChevronRight, AlertTriangle } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, ResponsiveContainer, Tooltip, ReferenceLine } from "recharts";

const BACKEND_URL = process.env.REACT_APP_BACKEND_URL;
const API = `${BACKEND_URL}/api`;

// ============= THREAT GAUGE =============
function ThreatGauge({ score, level }) {
  const radius = 70;
  const circumference = 2 * Math.PI * radius;
  const progress = (score / 100) * circumference;
  const color = level === "RED" ? "#ef4444" : level === "ORANGE" ? "#f97316" : level === "YELLOW" ? "#f59e0b" : "#22c55e";
  const glowClass = level === "RED" ? "glow-red" : level === "ORANGE" ? "glow-orange" : level === "YELLOW" ? "glow-yellow" : "glow-green";

  return (
    <div className={`detector-card p-6 flex flex-col items-center justify-center ${glowClass}`} data-testid="threat-gauge">
      <p className="text-xs font-mono uppercase tracking-widest text-zinc-500 mb-4">Threat Level</p>
      <div className="relative w-44 h-44">
        <svg className="w-full h-full -rotate-90" viewBox="0 0 160 160">
          <circle cx="80" cy="80" r={radius} fill="none" stroke="#27272a" strokeWidth="8" />
          <circle cx="80" cy="80" r={radius} fill="none" stroke={color} strokeWidth="8" strokeDasharray={circumference} strokeDashoffset={circumference - progress} strokeLinecap="round" className="transition-all duration-700 ease-out" />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="font-mono text-4xl font-bold" style={{ color }} data-testid="threat-score">{score}</span>
          <span className="font-mono text-xs text-zinc-500">/100</span>
        </div>
      </div>
      <div className="mt-4 flex items-center gap-2">
        {level === "GREEN" ? <ShieldCheck size={16} style={{ color }} /> : <ShieldAlert size={16} style={{ color }} />}
        <span className="threat-badge" style={{ background: `${color}20`, color }} data-testid="threat-level-badge">{level}</span>
      </div>
    </div>
  );
}

// ============= CELL INFO CARD =============
function CellInfoCard({ cell }) {
  if (!cell) return null;
  const rows = [
    { label: "Operator", value: cell.operator_name || "---", icon: <Wifi size={12} className="text-zinc-500" /> },
    { label: "Network", value: cell.network_type || "---", icon: <Signal size={12} className="text-zinc-500" /> },
    { label: "LAC/TAC", value: `${cell.lac}/${cell.tac}`, icon: <Radio size={12} className="text-zinc-500" /> },
    { label: "Cell ID", value: String(cell.cid), icon: <MapPin size={12} className="text-zinc-500" /> },
    { label: "EARFCN", value: cell.earfcn > 0 ? String(cell.earfcn) : "---" },
    { label: "PCI", value: cell.pci >= 0 ? String(cell.pci) : "---" },
    { label: "RSRP", value: `${cell.rsrp} dBm` },
    { label: "RSRQ", value: `${cell.rsrq} dB` },
    { label: "Roaming", value: cell.roaming ? "YES" : "NO" },
  ];

  return (
    <div className="detector-card p-5" data-testid="cell-info-card">
      <div className="flex items-center gap-2 mb-4">
        <Radio size={14} className="text-emerald-500" />
        <h3 className="text-xs font-mono uppercase tracking-widest text-zinc-500">Cell Tower</h3>
      </div>
      <div className="space-y-2">
        {rows.map((r) => (
          <div key={r.label} className="flex items-center justify-between py-1 border-b border-zinc-800/50 last:border-0">
            <span className="text-xs text-zinc-500 flex items-center gap-1.5">{r.icon}{r.label}</span>
            <span className="font-mono text-xs text-zinc-200" data-testid={`cell-${r.label.toLowerCase().replace(/[/ ]/g, '-')}`}>{r.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ============= ENCRYPTION STATUS =============
function EncryptionCard({ cell }) {
  if (!cell) return null;
  const isEncrypted = cell.cipher_status === "ENCRYPTED";
  const color = isEncrypted ? "#22c55e" : cell.cipher_status === "DOWNGRADED" ? "#f97316" : "#ef4444";
  const Icon = isEncrypted ? Lock : Unlock;

  return (
    <div className="detector-card p-5" data-testid="encryption-card">
      <div className="flex items-center gap-2 mb-4">
        <Lock size={14} className="text-emerald-500" />
        <h3 className="text-xs font-mono uppercase tracking-widest text-zinc-500">Encryption</h3>
      </div>
      <div className="flex items-center gap-3 mb-3">
        <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ background: `${color}15` }}>
          <Icon size={20} style={{ color }} />
        </div>
        <div>
          <p className="font-mono text-sm font-semibold" style={{ color }} data-testid="cipher-status">{cell.cipher_status}</p>
          <p className="font-mono text-xs text-zinc-500" data-testid="cipher-algorithm">{cell.cipher_algorithm || "---"}</p>
        </div>
      </div>
      <div className="flex gap-2 mt-3">
        {["A5/0", "A5/1", "A5/3"].map((algo) => (
          <span key={algo} className={`font-mono text-[10px] px-2 py-1 rounded ${cell.cipher_algorithm === algo ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-zinc-800 text-zinc-600'}`}>{algo}</span>
        ))}
      </div>
    </div>
  );
}

// ============= SIGNAL CHART =============
function SignalChart({ data }) {
  const chartData = (data || []).map((d, i) => ({
    idx: i,
    signal: d.signal_strength,
    type: d.network_type,
  }));

  return (
    <div className="detector-card p-5" data-testid="signal-chart">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Activity size={14} className="text-emerald-500" />
          <h3 className="text-xs font-mono uppercase tracking-widest text-zinc-500">Signal Strength</h3>
        </div>
        {chartData.length > 0 && (
          <span className="font-mono text-xs text-zinc-400">{chartData[chartData.length - 1]?.signal} dBm</span>
        )}
      </div>
      <div className="h-48">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData} margin={{ top: 5, right: 5, bottom: 5, left: -15 }}>
            <XAxis dataKey="idx" hide />
            <YAxis domain={[-120, -40]} tick={{ fill: '#71717a', fontSize: 10, fontFamily: 'JetBrains Mono' }} tickLine={false} axisLine={false} />
            <ReferenceLine y={-80} stroke="#27272a" strokeDasharray="3 3" />
            <ReferenceLine y={-100} stroke="#27272a" strokeDasharray="3 3" />
            <Tooltip content={({ active, payload }) => {
              if (active && payload?.[0]) {
                return (
                  <div className="bg-zinc-900 border border-zinc-700 rounded px-3 py-2">
                    <p className="font-mono text-xs text-emerald-400">{payload[0].value} dBm</p>
                  </div>
                );
              }
              return null;
            }} />
            <Line type="monotone" dataKey="signal" stroke="#10b981" strokeWidth={2} dot={false} activeDot={{ r: 3, fill: '#10b981' }} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

// ============= EVENT LOG =============
function EventLog({ events }) {
  const levelColor = (l) => l === "RED" ? "#ef4444" : l === "ORANGE" ? "#f97316" : l === "YELLOW" ? "#f59e0b" : "#22c55e";

  return (
    <div className="detector-card p-5" data-testid="event-log">
      <div className="flex items-center gap-2 mb-4">
        <List size={14} className="text-emerald-500" />
        <h3 className="text-xs font-mono uppercase tracking-widest text-zinc-500">Detection Log</h3>
        <span className="ml-auto font-mono text-[10px] text-zinc-600">{events?.length || 0} events</span>
      </div>
      <div className="space-y-1 max-h-64 overflow-y-auto">
        {(!events || events.length === 0) ? (
          <p className="text-xs text-zinc-600 font-mono py-4 text-center">No threats detected</p>
        ) : (
          events.slice(0, 15).map((evt, i) => {
            const color = levelColor(evt.threat_level);
            const time = evt.timestamp ? new Date(evt.timestamp).toLocaleTimeString() : "";
            return (
              <div key={evt.id || i} className="flex items-start gap-2 py-2 border-b border-zinc-800/50 last:border-0 fade-in" style={{ animationDelay: `${i * 30}ms` }} data-testid={`event-${i}`}>
                <div className="w-1.5 h-1.5 rounded-full mt-1.5 shrink-0" style={{ background: color }} />
                <div className="flex-1 min-w-0">
                  <p className="font-mono text-xs text-zinc-300 truncate">{evt.description}</p>
                  <p className="font-mono text-[10px] text-zinc-600 mt-0.5">{time} · {evt.network_type}</p>
                </div>
                <span className="threat-badge shrink-0" style={{ background: `${color}15`, color, fontSize: '9px', padding: '2px 6px' }}>{evt.threat_level}</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

// ============= SESSION BAR =============
function SessionBar({ session, isMonitoring, onToggle, onClear }) {
  return (
    <div className="detector-card px-5 py-3 flex items-center justify-between" data-testid="session-bar">
      <div className="flex items-center gap-4">
        <div className={`w-2 h-2 rounded-full ${isMonitoring ? 'bg-emerald-500 animate-pulse-glow' : 'bg-zinc-600'}`} />
        <span className="font-mono text-xs text-zinc-400">
          {isMonitoring ? "MONITORING" : "PAUSED"} · {session?.total_scans || 0} scans · {session?.threats_detected || 0} threats
        </span>
      </div>
      <div className="flex items-center gap-2">
        <button onClick={onClear} className="flex items-center gap-1.5 text-xs text-zinc-500 hover:text-red-400 transition-colors px-3 py-1.5 rounded hover:bg-zinc-800" data-testid="clear-data-btn">
          <Trash2 size={12} /> Clear
        </button>
        <button onClick={onToggle} className={`flex items-center gap-1.5 text-xs px-3 py-1.5 rounded font-mono transition-all ${isMonitoring ? 'bg-red-500/10 text-red-400 hover:bg-red-500/20' : 'bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20'}`} data-testid="monitor-toggle-btn">
          {isMonitoring ? <><PowerOff size={12} /> STOP</> : <><Power size={12} /> START</>}
        </button>
      </div>
    </div>
  );
}

// ============= STATS ROW =============
function StatsRow({ analysis }) {
  if (!analysis) return null;
  const items = [
    { label: "Encryption", value: analysis.encryption_score, max: 100 },
    { label: "Cell Consistency", value: analysis.cell_consistency_score, max: 100 },
    { label: "Signal Anomaly", value: analysis.signal_anomaly_score, max: 100 },
    { label: "Protocol", value: analysis.protocol_anomaly_score, max: 100 },
  ];
  const barColor = (v) => v > 50 ? "#ef4444" : v > 20 ? "#f59e0b" : "#22c55e";

  return (
    <div className="detector-card p-5" data-testid="stats-row">
      <div className="flex items-center gap-2 mb-4">
        <AlertTriangle size={14} className="text-emerald-500" />
        <h3 className="text-xs font-mono uppercase tracking-widest text-zinc-500">Threat Breakdown</h3>
      </div>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {items.map((item) => (
          <div key={item.label}>
            <div className="flex justify-between items-center mb-1">
              <span className="text-[10px] text-zinc-500 uppercase tracking-wider">{item.label}</span>
              <span className="font-mono text-xs" style={{ color: barColor(item.value) }}>{item.value}</span>
            </div>
            <div className="w-full h-1.5 bg-zinc-800 rounded-full overflow-hidden">
              <div className="h-full rounded-full transition-all duration-500" style={{ width: `${Math.min(item.value, 100)}%`, background: barColor(item.value) }} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ============= MAIN APP =============
export default function App() {
  const [dashboard, setDashboard] = useState(null);
  const [isMonitoring, setIsMonitoring] = useState(true);
  const [page, setPage] = useState("dashboard");
  const [history, setHistory] = useState([]);
  const [settings, setSettings] = useState(null);
  const intervalRef = useRef(null);

  const fetchDashboard = useCallback(async () => {
    try {
      const { data } = await axios.get(`${API}/dashboard`);
      setDashboard(data);
    } catch (e) {
      console.error("Dashboard fetch error:", e);
    }
  }, []);

  const fetchHistory = useCallback(async () => {
    try {
      const { data } = await axios.get(`${API}/history?limit=100`);
      setHistory(data);
    } catch (e) {
      console.error("History fetch error:", e);
    }
  }, []);

  const fetchSettings = useCallback(async () => {
    try {
      const { data } = await axios.get(`${API}/settings`);
      setSettings(data);
    } catch (e) {
      console.error("Settings fetch error:", e);
    }
  }, []);

  useEffect(() => {
    fetchDashboard();
  }, [fetchDashboard]);

  useEffect(() => {
    if (isMonitoring) {
      intervalRef.current = setInterval(fetchDashboard, 5000);
    }
    return () => { if (intervalRef.current) clearInterval(intervalRef.current); };
  }, [isMonitoring, fetchDashboard]);

  useEffect(() => {
    if (page === "history") fetchHistory();
    if (page === "settings") fetchSettings();
  }, [page, fetchHistory, fetchSettings]);

  const toggleMonitoring = async () => {
    try {
      if (isMonitoring) {
        await axios.post(`${API}/session/stop`);
      } else {
        await axios.post(`${API}/session/start`);
        fetchDashboard();
      }
      setIsMonitoring(!isMonitoring);
    } catch (e) { console.error(e); }
  };

  const clearData = async () => {
    try {
      await axios.delete(`${API}/data`);
      fetchDashboard();
    } catch (e) { console.error(e); }
  };

  const updateSettings = async (newSettings) => {
    try {
      const { data } = await axios.put(`${API}/settings`, newSettings);
      setSettings(data);
    } catch (e) { console.error(e); }
  };

  return (
    <div className="min-h-screen bg-[#09090b] relative">
      <div className="scanline-overlay" />

      {/* Header */}
      <header className="border-b border-zinc-800 px-6 py-3 flex items-center justify-between sticky top-0 z-50 bg-[#09090b]/90 backdrop-blur-sm" data-testid="app-header">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-md bg-emerald-500/10 flex items-center justify-center">
            <ShieldCheck size={18} className="text-emerald-500" />
          </div>
          <div>
            <h1 className="font-mono text-sm font-bold tracking-tight text-zinc-100">IMSI DETECTOR</h1>
            <p className="text-[10px] text-zinc-600 font-mono">CELL TOWER MONITORING SYSTEM</p>
          </div>
        </div>
        <nav className="flex items-center gap-1" data-testid="nav-tabs">
          {[
            { id: "dashboard", label: "Dashboard", icon: <Activity size={14} /> },
            { id: "history", label: "History", icon: <List size={14} /> },
            { id: "settings", label: "Settings", icon: <Settings size={14} /> },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setPage(tab.id)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono transition-all ${page === tab.id ? 'bg-emerald-500/10 text-emerald-400' : 'text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800/50'}`}
              data-testid={`nav-${tab.id}`}
            >
              {tab.icon} {tab.label}
            </button>
          ))}
        </nav>
      </header>

      {/* Content */}
      <main className="p-4 md:p-6 max-w-[1400px] mx-auto">
        {page === "dashboard" && (
          <div className="space-y-4 fade-in">
            <SessionBar session={dashboard?.session} isMonitoring={isMonitoring} onToggle={toggleMonitoring} onClear={clearData} />
            <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
              <div className="md:col-span-3">
                <ThreatGauge score={dashboard?.threat_analysis?.overall_score || 0} level={dashboard?.threat_analysis?.threat_level || "GREEN"} />
              </div>
              <div className="md:col-span-6">
                <SignalChart data={dashboard?.signal_history} />
              </div>
              <div className="md:col-span-3">
                <EncryptionCard cell={dashboard?.current_cell} />
              </div>
            </div>
            <StatsRow analysis={dashboard?.threat_analysis} />
            <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
              <div className="md:col-span-4">
                <CellInfoCard cell={dashboard?.current_cell} />
              </div>
              <div className="md:col-span-8">
                <EventLog events={dashboard?.recent_events} />
              </div>
            </div>
          </div>
        )}

        {page === "history" && <HistoryPage events={history} />}
        {page === "settings" && <SettingsPage settings={settings} onSave={updateSettings} onClear={clearData} />}
      </main>
    </div>
  );
}

// ============= HISTORY PAGE =============
function HistoryPage({ events }) {
  const levelColor = (l) => l === "RED" ? "#ef4444" : l === "ORANGE" ? "#f97316" : l === "YELLOW" ? "#f59e0b" : "#22c55e";

  return (
    <div className="space-y-4 fade-in" data-testid="history-page">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-lg font-bold text-zinc-100">Threat History</h2>
        <span className="font-mono text-xs text-zinc-500">{events.length} events</span>
      </div>
      <div className="detector-card divide-y divide-zinc-800">
        {events.length === 0 ? (
          <div className="py-12 text-center">
            <ShieldCheck size={32} className="text-zinc-700 mx-auto mb-3" />
            <p className="text-sm text-zinc-500">No threat events recorded</p>
          </div>
        ) : (
          events.map((evt, i) => {
            const color = levelColor(evt.threat_level);
            const time = evt.timestamp ? new Date(evt.timestamp).toLocaleString() : "";
            return (
              <div key={evt.id || i} className="p-4 flex items-start gap-3 hover:bg-zinc-800/30 transition-colors" data-testid={`history-event-${i}`}>
                <div className="w-2 h-2 rounded-full mt-1.5 shrink-0" style={{ background: color }} />
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="threat-badge" style={{ background: `${color}15`, color }}>{evt.threat_level}</span>
                    <span className="font-mono text-[10px] text-zinc-600">{time}</span>
                  </div>
                  <p className="font-mono text-xs text-zinc-300 mt-1">{evt.description}</p>
                  {evt.recommended_action && (
                    <p className="text-[10px] text-zinc-500 mt-1 flex items-center gap-1">
                      <ChevronRight size={10} /> {evt.recommended_action}
                    </p>
                  )}
                </div>
                <span className="font-mono text-xs text-zinc-600">{evt.network_type}</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

// ============= SETTINGS PAGE =============
function SettingsPage({ settings, onSave, onClear }) {
  const [local, setLocal] = useState(settings);
  useEffect(() => { if (settings) setLocal(settings); }, [settings]);

  if (!local) return <div className="text-zinc-500 text-center py-12 font-mono text-sm">Loading settings...</div>;

  const toggle = (key) => {
    const updated = { ...local, [key]: !local[key] };
    setLocal(updated);
    onSave(updated);
  };

  return (
    <div className="space-y-6 max-w-2xl mx-auto fade-in" data-testid="settings-page">
      <h2 className="font-mono text-lg font-bold text-zinc-100">Settings</h2>

      <div className="detector-card p-5 space-y-4">
        <h3 className="text-xs font-mono uppercase tracking-widest text-emerald-500">Monitoring</h3>
        <SettingToggle label="Background Monitoring" desc="Continue monitoring when app is in background" checked={local.background_monitoring} onChange={() => toggle("background_monitoring")} testId="setting-background" />
        <SettingToggle label="Notifications" desc="Receive alerts for detected threats" checked={local.notifications_enabled} onChange={() => toggle("notifications_enabled")} testId="setting-notifications" />
      </div>

      <div className="detector-card p-5 space-y-4">
        <h3 className="text-xs font-mono uppercase tracking-widest text-emerald-500">Detection</h3>
        <SettingToggle label="Auto-Reset Baseline" desc="Reset baseline when location changes significantly" checked={local.auto_reset_baseline} onChange={() => toggle("auto_reset_baseline")} testId="setting-autoreset" />
        <div>
          <label className="text-xs text-zinc-400 block mb-1">Scan Interval</label>
          <select value={local.scan_interval_seconds} onChange={(e) => { const u = { ...local, scan_interval_seconds: Number(e.target.value) }; setLocal(u); onSave(u); }} className="bg-zinc-800 border border-zinc-700 rounded px-3 py-1.5 text-xs font-mono text-zinc-300 outline-none focus:border-emerald-500" data-testid="setting-interval">
            <option value={3}>3 seconds</option>
            <option value={5}>5 seconds</option>
            <option value={10}>10 seconds</option>
            <option value={30}>30 seconds</option>
          </select>
        </div>
      </div>

      <div className="detector-card p-5 space-y-4">
        <h3 className="text-xs font-mono uppercase tracking-widest text-red-400">Danger Zone</h3>
        <button onClick={onClear} className="flex items-center gap-2 text-xs text-red-400 hover:text-red-300 bg-red-500/10 hover:bg-red-500/20 px-4 py-2 rounded font-mono transition-colors" data-testid="clear-all-data-btn">
          <Trash2 size={14} /> Clear All Data
        </button>
        <p className="text-[10px] text-zinc-600">This will permanently delete all recorded cell data and threat history.</p>
      </div>

      <div className="detector-card p-5">
        <h3 className="text-xs font-mono uppercase tracking-widest text-zinc-500 mb-3">About</h3>
        <div className="space-y-2">
          {[["App", "IMSI Catcher Detector"], ["Version", "1.0.0"], ["Engine", "Threat Analysis v1"], ["Platform", "Web"]].map(([k, v]) => (
            <div key={k} className="flex justify-between py-1 border-b border-zinc-800/50 last:border-0">
              <span className="text-xs text-zinc-500">{k}</span>
              <span className="font-mono text-xs text-zinc-300">{v}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function SettingToggle({ label, desc, checked, onChange, testId }) {
  return (
    <div className="flex items-center justify-between py-2" data-testid={testId}>
      <div>
        <p className="text-sm text-zinc-200">{label}</p>
        <p className="text-[10px] text-zinc-500 mt-0.5">{desc}</p>
      </div>
      <button onClick={onChange} className={`w-10 h-5 rounded-full transition-colors relative ${checked ? 'bg-emerald-500' : 'bg-zinc-700'}`} data-testid={`${testId}-toggle`}>
        <div className={`w-4 h-4 bg-white rounded-full absolute top-0.5 transition-transform ${checked ? 'translate-x-5' : 'translate-x-0.5'}`} />
      </button>
    </div>
  );
}
