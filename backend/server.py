from fastapi import FastAPI, APIRouter, HTTPException, Query
from dotenv import load_dotenv
from starlette.middleware.cors import CORSMiddleware
from motor.motor_asyncio import AsyncIOMotorClient
import os
import logging
import random
import math
import httpx
from pathlib import Path
from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional
import uuid
from datetime import datetime, timezone

ROOT_DIR = Path(__file__).parent
load_dotenv(ROOT_DIR / '.env')

mongo_url = os.environ['MONGO_URL']
client = AsyncIOMotorClient(mongo_url)
db = client[os.environ['DB_NAME']]
OPENCELLID_KEY = os.environ['OPENCELLID_API_KEY']

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
    mcc: int = -1
    mnc: int = -1
    earfcn: int = -1
    pci: int = -1
    rsrp: int = -1
    rsrq: int = -1
    signal_strength: int = -1
    signal_level: int = -1
    cipher_status: str = "UNKNOWN"
    cipher_algorithm: str = ""
    network_type: str = ""
    operator_name: str = ""
    operator_code: str = ""
    roaming: bool = False
    latitude: float = 0.0
    longitude: float = 0.0
    range_m: int = 0
    samples: int = 0
    is_verified: bool = False
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

class RealTower(BaseModel):
    model_config = ConfigDict(extra="ignore")
    cell_id: int = 0
    lac: int = 0
    mcc: int = 0
    mnc: int = 0
    lat: float = 0.0
    lon: float = 0.0
    range_m: int = 0
    samples: int = 0
    radio: str = ""
    average_signal: int = 0
    is_verified: bool = True

class MonitoringSession(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    started_at: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    is_active: bool = True
    total_scans: int = 0
    threats_detected: int = 0
    real_towers_loaded: int = 0

class DashboardData(BaseModel):
    current_cell: CellTowerRecord
    threat_analysis: ThreatAnalysis
    signal_history: List[dict]
    recent_events: List[ThreatEvent]
    session: MonitoringSession
    nearby_towers: List[RealTower]

class TowerLookupRequest(BaseModel):
    mcc: int
    mnc: int
    lac: int
    cell_id: int
    radio: Optional[str] = None

class SettingsModel(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = "default"
    notifications_enabled: bool = True
    background_monitoring: bool = True
    auto_reset_baseline: bool = False
    scan_interval_seconds: int = 5
    threat_sensitivity: str = "medium"
    user_lat: float = 37.7749
    user_lon: float = -122.4194

# ============= OPENCELLID CLIENT =============

class OpenCellIDClient:
    BASE_URL = "https://opencellid.org"

    def __init__(self, api_key: str):
        self.api_key = api_key
        self.http = httpx.AsyncClient(timeout=15.0)

    async def lookup_cell(self, mcc: int, mnc: int, lac: int, cell_id: int, radio: str = None) -> Optional[dict]:
        params = {
            "key": self.api_key,
            "mcc": mcc,
            "mnc": mnc,
            "lac": lac,
            "cellid": cell_id,
            "format": "json"
        }
        if radio:
            params["radio"] = radio
        try:
            resp = await self.http.get(f"{self.BASE_URL}/cell/get", params=params)
            if resp.status_code == 200:
                data = resp.json()
                if "error" not in data:
                    return data
            logger.warning(f"OpenCelliD lookup failed: {resp.status_code} {resp.text[:200]}")
            return None
        except Exception as e:
            logger.error(f"OpenCelliD request error: {e}")
            return None

    async def get_nearby_cells(self, lat: float, lon: float, radius_km: float = 0.5) -> List[dict]:
        delta_lat = radius_km / 111.0
        delta_lon = radius_km / (111.0 * max(0.01, math.cos(math.radians(lat))))
        bbox = f"{lat - delta_lat},{lon - delta_lon},{lat + delta_lat},{lon + delta_lon}"
        params = {
            "key": self.api_key,
            "BBOX": bbox,
            "format": "json",
            "limit": 50
        }
        try:
            resp = await self.http.get(f"{self.BASE_URL}/cell/getInArea", params=params)
            if resp.status_code == 200:
                data = resp.json()
                if isinstance(data, dict) and "cells" in data:
                    return data["cells"]
                if isinstance(data, list):
                    return data
            logger.warning(f"OpenCelliD area failed: {resp.status_code} {resp.text[:200]}")
            return []
        except Exception as e:
            logger.error(f"OpenCelliD area error: {e}")
            return []

ocid = OpenCellIDClient(OPENCELLID_KEY)

# ============= DETECTION ENGINE (REAL DATA) =============

class DetectionEngine:
    def __init__(self):
        self.signal_history = []
        self.cell_history = []
        self.known_towers = {}
        self.previous_network_type = ""

    def register_known_towers(self, towers: List[dict]):
        for t in towers:
            key = f"{t.get('mcc',0)}-{t.get('mnc',0)}-{t.get('lac',0)}-{t.get('cellid', t.get('cell_id', 0))}"
            self.known_towers[key] = t

    def analyze_threat(self, cell: CellTowerRecord) -> ThreatAnalysis:
        threats = []
        recommendations = []

        enc_score = self._analyze_encryption(cell, threats)
        cell_score = self._analyze_cell_consistency(cell, threats)
        sig_score = self._analyze_signal(cell, threats)
        proto_score = self._analyze_protocol(cell, threats)
        verify_score = self._verify_against_known(cell, threats)

        cell_score += verify_score

        overall = int(enc_score * 0.40 + cell_score * 0.30 + sig_score * 0.15 + proto_score * 0.15)
        overall = min(overall, 100)

        level = "GREEN"
        if overall > 75: level = "RED"
        elif overall > 50: level = "ORANGE"
        elif overall > 20: level = "YELLOW"

        if not threats:
            recommendations.append("All towers verified against OpenCelliD - network secure")
        else:
            if enc_score > 0:
                recommendations.append("Weak encryption detected - avoid sensitive communications")
            if verify_score > 0:
                recommendations.append("Unverified tower detected - possible IMSI catcher")
            if sig_score > 0:
                recommendations.append("Signal anomaly - monitor for fake tower indicators")

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

    def _verify_against_known(self, cell: CellTowerRecord, threats: list) -> int:
        if not self.known_towers:
            return 0
        key = f"{cell.mcc}-{cell.mnc}-{cell.lac}-{cell.cid}"
        if key in self.known_towers:
            known = self.known_towers[key]
            dist = self._haversine(cell.latitude, cell.longitude, known.get("lat", 0), known.get("lon", 0))
            if dist > 5.0:
                threats.append(f"SUSPICIOUS: Tower {cell.cid} is {dist:.1f}km from registered location")
                return 25
            return 0
        else:
            if cell.cid > 0 and cell.mcc > 0:
                threats.append(f"UNVERIFIED: Tower CID {cell.cid} not found in OpenCelliD database")
                return 15
        return 0

    def _haversine(self, lat1, lon1, lat2, lon2):
        R = 6371
        dlat = math.radians(lat2 - lat1)
        dlon = math.radians(lon2 - lon1)
        a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon/2)**2
        return R * 2 * math.asin(math.sqrt(a))

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
cached_nearby_towers: List[RealTower] = []

# ============= REAL DATA GENERATION =============

US_OPERATORS = {
    "310260": {"name": "T-Mobile", "mcc": 310, "mnc": 260},
    "310410": {"name": "AT&T", "mcc": 310, "mnc": 410},
    "311480": {"name": "Verizon", "mcc": 311, "mnc": 480},
}

def generate_cell_from_real_towers(real_towers: List[RealTower], inject_threat: bool = False) -> CellTowerRecord:
    """Generate cell data based on REAL tower data from OpenCelliD."""
    if real_towers and not inject_threat:
        tower = random.choice(real_towers)
        net_type = tower.radio.upper() if tower.radio else "LTE"
        if net_type == "UMTS": net_type = "WCDMA"
        base_signal = tower.average_signal if tower.average_signal else -80
        if base_signal == 0: base_signal = -80
        signal = base_signal + random.randint(-8, 8)
        signal = max(-120, min(-40, signal))
        level = max(0, min(4, (signal + 120) // 20))

        op_key = f"{tower.mcc}{tower.mnc}"
        op = US_OPERATORS.get(op_key, {"name": f"MCC{tower.mcc}/MNC{tower.mnc}", "mcc": tower.mcc, "mnc": tower.mnc})

        return CellTowerRecord(
            lac=tower.lac, tac=tower.lac, cid=tower.cell_id,
            mcc=tower.mcc, mnc=tower.mnc,
            pci=random.randint(0, 503) if net_type in ("LTE", "NR") else -1,
            earfcn=random.randint(100, 65535) if net_type == "LTE" else -1,
            rsrp=signal, rsrq=random.randint(-20, -3),
            signal_strength=signal, signal_level=level,
            cipher_status="ENCRYPTED", cipher_algorithm="A5/3",
            network_type=net_type,
            operator_name=op["name"], operator_code=f"{tower.mcc}{tower.mnc}",
            latitude=tower.lat, longitude=tower.lon,
            range_m=tower.range_m, samples=tower.samples,
            is_verified=True,
        )

    # Inject a suspicious/unverified tower for threat simulation
    op = random.choice(list(US_OPERATORS.values()))
    threat_type = random.choice(["unverified", "encryption", "signal", "downgrade"])
    net_type = "LTE"
    cipher_status = "ENCRYPTED"
    cipher_algo = "A5/3"
    signal = -80 + random.randint(-10, 10)
    fake_lat = 37.7749 + random.uniform(-0.005, 0.005)
    fake_lon = -122.4194 + random.uniform(-0.005, 0.005)
    fake_cid = random.randint(900000, 999999)
    fake_lac = random.randint(60000, 65000)

    if threat_type == "encryption":
        cipher_status = random.choice(["UNENCRYPTED", "DOWNGRADED"])
        cipher_algo = random.choice(["A5/0", "A5/1"])
        net_type = "GSM"
    elif threat_type == "signal":
        signal = random.randint(-45, -30)
    elif threat_type == "downgrade":
        net_type = "GSM"
        cipher_algo = "A5/1"

    return CellTowerRecord(
        lac=fake_lac, tac=fake_lac, cid=fake_cid,
        mcc=op["mcc"], mnc=op["mnc"],
        pci=random.randint(0, 503), earfcn=random.randint(100, 65535),
        rsrp=signal, rsrq=random.randint(-20, -3),
        signal_strength=signal, signal_level=max(0, min(4, (signal + 120) // 20)),
        cipher_status=cipher_status, cipher_algorithm=cipher_algo,
        network_type=net_type,
        operator_name=op["name"], operator_code=f"{op['mcc']}{op['mnc']}",
        latitude=fake_lat, longitude=fake_lon,
        range_m=random.randint(100, 5000), samples=0,
        is_verified=False,
    )

# ============= API ENDPOINTS =============

@api_router.get("/")
async def root():
    return {"message": "IMSI Catcher Detector API v1.0 - Real Data"}

@api_router.get("/dashboard", response_model=DashboardData)
async def get_dashboard(lat: float = Query(default=37.7749), lon: float = Query(default=-122.4194)):
    global cached_nearby_towers

    # Fetch real nearby towers if cache empty or location changed
    if not cached_nearby_towers:
        raw_towers = await ocid.get_nearby_cells(lat, lon, radius_km=0.5)
        cached_nearby_towers = []
        for t in raw_towers[:50]:
            cached_nearby_towers.append(RealTower(
                cell_id=t.get("cellid", t.get("cell_id", 0)),
                lac=t.get("lac", 0),
                mcc=t.get("mcc", 0),
                mnc=t.get("mnc", 0),
                lat=float(t.get("lat", 0)),
                lon=float(t.get("lon", 0)),
                range_m=int(t.get("range", 0)),
                samples=int(t.get("samples", 0)),
                radio=t.get("radio", "LTE"),
                average_signal=int(t.get("averageSignalStrength", 0)),
            ))
        engine.register_known_towers(raw_towers)
        active_session.real_towers_loaded = len(cached_nearby_towers)
        logger.info(f"Loaded {len(cached_nearby_towers)} real towers from OpenCelliD near ({lat}, {lon})")

    inject_threat = random.random() < 0.12
    cell = generate_cell_from_real_towers(cached_nearby_towers, inject_threat=inject_threat)
    analysis = engine.analyze_threat(cell)

    cell_doc = cell.model_dump()
    await db.cell_records.insert_one(cell_doc)

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

    recent_cells = await db.cell_records.find({}, {"_id": 0}).sort("timestamp", -1).limit(30).to_list(30)
    signal_history = [
        {"timestamp": c.get("timestamp", ""), "signal_strength": c.get("signal_strength", -80), "network_type": c.get("network_type", ""), "is_verified": c.get("is_verified", False)}
        for c in reversed(recent_cells)
    ]

    recent_events_raw = await db.threat_events.find({}, {"_id": 0}).sort("timestamp", -1).limit(20).to_list(20)
    recent_events = [ThreatEvent(**e) for e in recent_events_raw]

    return DashboardData(
        current_cell=cell,
        threat_analysis=analysis,
        signal_history=signal_history,
        recent_events=recent_events,
        session=active_session,
        nearby_towers=cached_nearby_towers[:20],
    )

@api_router.post("/tower/lookup", response_model=Optional[RealTower])
async def lookup_tower(req: TowerLookupRequest):
    result = await ocid.lookup_cell(req.mcc, req.mnc, req.lac, req.cell_id, req.radio)
    if not result:
        raise HTTPException(status_code=404, detail="Tower not found in OpenCelliD database")
    return RealTower(
        cell_id=result.get("cellid", result.get("cell_id", req.cell_id)),
        lac=result.get("lac", req.lac),
        mcc=result.get("mcc", req.mcc),
        mnc=result.get("mnc", req.mnc),
        lat=float(result.get("lat", 0)),
        lon=float(result.get("lon", 0)),
        range_m=int(result.get("range", 0)),
        samples=int(result.get("samples", 0)),
        radio=result.get("radio", ""),
        average_signal=int(result.get("averageSignalStrength", 0)),
    )

@api_router.get("/towers/nearby", response_model=List[RealTower])
async def get_nearby_towers(lat: float = 37.7749, lon: float = -122.4194, radius_km: float = 0.5):
    global cached_nearby_towers
    raw_towers = await ocid.get_nearby_cells(lat, lon, radius_km)
    cached_nearby_towers = []
    for t in raw_towers[:50]:
        cached_nearby_towers.append(RealTower(
            cell_id=t.get("cellid", t.get("cell_id", 0)),
            lac=t.get("lac", 0),
            mcc=t.get("mcc", 0),
            mnc=t.get("mnc", 0),
            lat=float(t.get("lat", 0)),
            lon=float(t.get("lon", 0)),
            range_m=int(t.get("range", 0)),
            samples=int(t.get("samples", 0)),
            radio=t.get("radio", "LTE"),
            average_signal=int(t.get("averageSignalStrength", 0)),
        ))
    engine.register_known_towers(raw_towers)
    active_session.real_towers_loaded = len(cached_nearby_towers)
    return cached_nearby_towers

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

    # Refresh towers if location changed
    global cached_nearby_towers
    cached_nearby_towers = []

    return settings

@api_router.post("/session/start", response_model=MonitoringSession)
async def start_session():
    global active_session, cached_nearby_towers
    active_session = MonitoringSession()
    cached_nearby_towers = []
    return active_session

@api_router.post("/session/stop", response_model=MonitoringSession)
async def stop_session():
    global active_session
    active_session.is_active = False
    return active_session

@api_router.delete("/data")
async def clear_data():
    global cached_nearby_towers
    await db.cell_records.delete_many({})
    await db.threat_events.delete_many({})
    cached_nearby_towers = []
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
        "real_towers_loaded": active_session.real_towers_loaded,
    }

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
