from fastapi import FastAPI, APIRouter, HTTPException, Query
from dotenv import load_dotenv
from starlette.middleware.cors import CORSMiddleware
from motor.motor_asyncio import AsyncIOMotorClient
import os
import logging
import random
import math
from pathlib import Path
from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional
import uuid
from datetime import datetime, timezone, timedelta

ROOT_DIR = Path(__file__).parent
load_dotenv(ROOT_DIR / '.env')

mongo_url = os.environ['MONGO_URL']
client = AsyncIOMotorClient(mongo_url)
db = client[os.environ['DB_NAME']]

app = FastAPI()
api_router = APIRouter(prefix="/api")

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

# ============= MODELS =============

class CellTowerRecord(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    lac: int = -1
    tac: int = -1
    cid: int = -1
    earfcn: int = -1
    pci: int = -1
    rsrp: int = -1
    rsrq: int = -1
    cqi: int = -1
    arfcn: int = -1
    bsic: int = -1
    rssi: int = -1
    signal_strength: int = -1
    signal_level: int = -1
    timing_advance: int = -1
    cipher_status: str = "UNKNOWN"
    cipher_algorithm: str = ""
    network_type: str = ""
    operator_name: str = ""
    operator_code: str = ""
    roaming: bool = False
    latitude: float = 0.0
    longitude: float = 0.0
    neighbor_cells: str = ""

class ThreatEvent(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    threat_type: str = ""
    severity: int = 0
    threat_level: str = "GREEN"
    description: str = ""
    recommended_action: str = ""
    network_type: str = ""

class ThreatAnalysis(BaseModel):
    overall_score: int = 0
    threat_level: str = "GREEN"
    encryption_score: int = 0
    cell_consistency_score: int = 0
    signal_anomaly_score: int = 0
    protocol_anomaly_score: int = 0
    detected_threats: List[str] = []
    recommendations: List[str] = []

class MonitoringSession(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    started_at: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    is_active: bool = True
    total_scans: int = 0
    threats_detected: int = 0

class DashboardData(BaseModel):
    current_cell: CellTowerRecord
    threat_analysis: ThreatAnalysis
    signal_history: List[dict]
    recent_events: List[ThreatEvent]
    session: MonitoringSession

class SettingsModel(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = "default"
    notifications_enabled: bool = True
    background_monitoring: bool = True
    auto_reset_baseline: bool = False
    scan_interval_seconds: int = 5
    threat_sensitivity: str = "medium"

# ============= DETECTION ENGINE =============

class DetectionEngine:
    def __init__(self):
        self.signal_history = []
        self.cell_history = []
        self.previous_network_type = ""

    def analyze_threat(self, cell: CellTowerRecord) -> ThreatAnalysis:
        threats = []
        recommendations = []

        enc_score = self._analyze_encryption(cell, threats)
        cell_score = self._analyze_cell_consistency(cell, threats)
        sig_score = self._analyze_signal(cell, threats)
        proto_score = self._analyze_protocol(cell, threats)

        overall = int(
            enc_score * 0.45 +
            cell_score * 0.30 +
            sig_score * 0.15 +
            proto_score * 0.10
        )
        overall = min(overall, 100)

        level = "GREEN"
        if overall > 75: level = "RED"
        elif overall > 50: level = "ORANGE"
        elif overall > 20: level = "YELLOW"

        if not threats:
            recommendations.append("Network appears secure - no threats detected")
        else:
            if enc_score > 0:
                recommendations.append("Ensure strong encryption (A5/3 or better)")
            if cell_score > 0:
                recommendations.append("Monitor cell tower changes")
            if sig_score > 0:
                recommendations.append("Check signal strength trends")

        self.cell_history.append(cell)
        if len(self.cell_history) > 100:
            self.cell_history.pop(0)
        self.previous_network_type = cell.network_type

        return ThreatAnalysis(
            overall_score=overall,
            threat_level=level,
            encryption_score=enc_score,
            cell_consistency_score=cell_score,
            signal_anomaly_score=sig_score,
            protocol_anomaly_score=proto_score,
            detected_threats=threats,
            recommendations=recommendations
        )

    def _analyze_encryption(self, cell: CellTowerRecord, threats: list) -> int:
        score = 0
        if cell.cipher_status == "UNENCRYPTED":
            score = 90
            threats.append("CRITICAL: No encryption detected (A5/0)")
        elif cell.cipher_status == "DOWNGRADED":
            score = 60
            threats.append("WARNING: Cipher downgrade detected")
        elif cell.network_type in ("GSM", "EDGE"):
            score = 30
            threats.append("WEAK: 2G network with limited encryption")
        return score

    def _analyze_cell_consistency(self, cell: CellTowerRecord, threats: list) -> int:
        score = 0
        if len(self.cell_history) < 2:
            return score

        prev = self.cell_history[-1]
        if prev.lac != cell.lac or prev.tac != cell.tac:
            score += 15
            threats.append(f"LAC/TAC changed: {prev.lac}/{prev.tac} -> {cell.lac}/{cell.tac}")
        if prev.cid != cell.cid and prev.lac == cell.lac:
            score += 10
            threats.append("Cell ID changed without LAC change")
        return score

    def _analyze_signal(self, cell: CellTowerRecord, threats: list) -> int:
        score = 0
        self.signal_history.append(cell.signal_strength)
        if len(self.signal_history) > 50:
            self.signal_history.pop(0)
        if len(self.signal_history) >= 2:
            change = abs(self.signal_history[-1] - self.signal_history[-2])
            if change > 15:
                score += 20
                threats.append(f"Sudden signal change: {change}dBm")
        if cell.signal_strength > -50:
            score += 10
            threats.append(f"Unusually strong signal: {cell.signal_strength}dBm")
        return score

    def _analyze_protocol(self, cell: CellTowerRecord, threats: list) -> int:
        score = 0
        if self.previous_network_type in ("LTE", "NR") and cell.network_type in ("GSM", "EDGE"):
            score += 30
            threats.append(f"Forced downgrade: {self.previous_network_type} -> {cell.network_type}")
        return score

engine = DetectionEngine()
active_session = MonitoringSession()

# ============= SIMULATION ENGINE =============

OPERATORS = [
    {"name": "T-Mobile", "code": "310260"},
    {"name": "AT&T", "code": "310410"},
    {"name": "Verizon", "code": "311480"},
]

def generate_cell_data(inject_threat: bool = False) -> CellTowerRecord:
    op = random.choice(OPERATORS)
    net_type = random.choices(["NR", "LTE", "WCDMA", "GSM"], weights=[35, 45, 15, 5])[0]

    base_rsrp = {"NR": -85, "LTE": -80, "WCDMA": -85, "GSM": -75}[net_type]
    signal = base_rsrp + random.randint(-15, 15)
    level = max(0, min(4, (signal + 120) // 20))

    cipher_status = "ENCRYPTED"
    cipher_algo = "A5/3"
    lac = random.randint(1000, 9999)
    tac = random.randint(1000, 9999)
    cid = random.randint(10000, 99999)

    if inject_threat:
        threat_type = random.choice(["encryption", "signal", "downgrade", "cell_change"])
        if threat_type == "encryption":
            cipher_status = random.choice(["UNENCRYPTED", "DOWNGRADED"])
            cipher_algo = random.choice(["A5/0", "A5/1"])
            net_type = "GSM"
        elif threat_type == "signal":
            signal = random.randint(-45, -30)
            level = 4
        elif threat_type == "downgrade":
            net_type = "GSM"
            cipher_algo = "A5/1"
        elif threat_type == "cell_change":
            lac = random.randint(50000, 65000)
            tac = random.randint(50000, 65000)

    return CellTowerRecord(
        lac=lac, tac=tac, cid=cid,
        earfcn=random.randint(100, 65535) if net_type == "LTE" else -1,
        pci=random.randint(0, 503) if net_type in ("LTE", "NR") else -1,
        rsrp=signal, rsrq=random.randint(-20, -3),
        signal_strength=signal, signal_level=level,
        cipher_status=cipher_status, cipher_algorithm=cipher_algo,
        network_type=net_type,
        operator_name=op["name"], operator_code=op["code"],
        latitude=37.7749 + random.uniform(-0.01, 0.01),
        longitude=-122.4194 + random.uniform(-0.01, 0.01),
    )

# ============= API ENDPOINTS =============

@api_router.get("/")
async def root():
    return {"message": "IMSI Catcher Detector API v1.0"}

@api_router.get("/dashboard", response_model=DashboardData)
async def get_dashboard():
    inject_threat = random.random() < 0.15
    cell = generate_cell_data(inject_threat=inject_threat)
    analysis = engine.analyze_threat(cell)

    # Store cell record
    cell_doc = cell.model_dump()
    await db.cell_records.insert_one(cell_doc)

    # Store threat events
    events_to_return = []
    if analysis.detected_threats:
        active_session.threats_detected += 1
        for threat_desc in analysis.detected_threats[:3]:
            evt = ThreatEvent(
                threat_type=threat_desc.split(":")[0].strip() if ":" in threat_desc else "ANOMALY",
                severity=analysis.overall_score,
                threat_level=analysis.threat_level,
                description=threat_desc,
                recommended_action=analysis.recommendations[0] if analysis.recommendations else "",
                network_type=cell.network_type,
            )
            events_to_return.append(evt)
            await db.threat_events.insert_one(evt.model_dump())

    active_session.total_scans += 1

    # Build signal history from last 30 records
    recent_cells = await db.cell_records.find({}, {"_id": 0}).sort("timestamp", -1).limit(30).to_list(30)
    signal_history = [
        {"timestamp": c.get("timestamp", ""), "signal_strength": c.get("signal_strength", -80), "network_type": c.get("network_type", "")}
        for c in reversed(recent_cells)
    ]

    # Get recent events
    recent_events_raw = await db.threat_events.find({}, {"_id": 0}).sort("timestamp", -1).limit(20).to_list(20)
    recent_events = [ThreatEvent(**e) for e in recent_events_raw]

    return DashboardData(
        current_cell=cell,
        threat_analysis=analysis,
        signal_history=signal_history,
        recent_events=recent_events,
        session=active_session,
    )

@api_router.get("/history", response_model=List[ThreatEvent])
async def get_history(skip: int = 0, limit: int = 50):
    events = await db.threat_events.find({}, {"_id": 0}).sort("timestamp", -1).skip(skip).limit(limit).to_list(limit)
    return [ThreatEvent(**e) for e in events]

@api_router.get("/cell-records", response_model=List[CellTowerRecord])
async def get_cell_records(limit: int = 100):
    records = await db.cell_records.find({}, {"_id": 0}).sort("timestamp", -1).limit(limit).to_list(limit)
    return [CellTowerRecord(**r) for r in records]

@api_router.get("/settings", response_model=SettingsModel)
async def get_settings():
    settings = await db.settings.find_one({"id": "default"}, {"_id": 0})
    if not settings:
        default = SettingsModel()
        await db.settings.insert_one(default.model_dump())
        return default
    return SettingsModel(**settings)

@api_router.put("/settings", response_model=SettingsModel)
async def update_settings(settings: SettingsModel):
    settings.id = "default"
    await db.settings.update_one({"id": "default"}, {"$set": settings.model_dump()}, upsert=True)
    return settings

@api_router.post("/session/start", response_model=MonitoringSession)
async def start_session():
    global active_session
    active_session = MonitoringSession()
    return active_session

@api_router.post("/session/stop", response_model=MonitoringSession)
async def stop_session():
    global active_session
    active_session.is_active = False
    return active_session

@api_router.delete("/data")
async def clear_data():
    await db.cell_records.delete_many({})
    await db.threat_events.delete_many({})
    return {"message": "All data cleared"}

@api_router.get("/stats")
async def get_stats():
    cell_count = await db.cell_records.count_documents({})
    threat_count = await db.threat_events.count_documents({})
    high_threats = await db.threat_events.count_documents({"threat_level": {"$in": ["ORANGE", "RED"]}})
    return {
        "total_scans": cell_count,
        "total_threats": threat_count,
        "high_severity_threats": high_threats,
        "session_active": active_session.is_active,
        "session_scans": active_session.total_scans,
    }

# Include router and middleware
app.include_router(api_router)

app.add_middleware(
    CORSMiddleware,
    allow_credentials=True,
    allow_origins=os.environ.get('CORS_ORIGINS', '*').split(','),
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.on_event("shutdown")
async def shutdown_db_client():
    client.close()
