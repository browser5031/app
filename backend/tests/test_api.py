"""
IMSI Catcher Detector API Tests
Tests all backend API endpoints for the cell tower monitoring application
"""
import pytest
import requests
import os

BASE_URL = os.environ.get('REACT_APP_BACKEND_URL', '').rstrip('/')

class TestRootEndpoint:
    """Test the root API endpoint"""
    
    def test_root_returns_welcome_message(self):
        """GET /api/ returns welcome message"""
        response = requests.get(f"{BASE_URL}/api/")
        assert response.status_code == 200
        data = response.json()
        assert "message" in data
        assert "IMSI Catcher Detector API" in data["message"]


class TestDashboardEndpoint:
    """Test the dashboard data endpoint"""
    
    def test_dashboard_returns_full_data(self):
        """GET /api/dashboard returns full dashboard data"""
        response = requests.get(f"{BASE_URL}/api/dashboard")
        assert response.status_code == 200
        data = response.json()
        
        # Check required fields
        assert "current_cell" in data
        assert "threat_analysis" in data
        assert "signal_history" in data
        assert "recent_events" in data
        assert "session" in data
        
        # Validate current_cell structure
        cell = data["current_cell"]
        assert "lac" in cell
        assert "tac" in cell
        assert "cid" in cell
        assert "cipher_status" in cell
        assert "network_type" in cell
        assert "operator_name" in cell
        
        # Validate threat_analysis structure
        analysis = data["threat_analysis"]
        assert "overall_score" in analysis
        assert "threat_level" in analysis
        assert analysis["threat_level"] in ["GREEN", "YELLOW", "ORANGE", "RED"]
        
        # Validate session structure
        session = data["session"]
        assert "is_active" in session
        assert "total_scans" in session


class TestHistoryEndpoint:
    """Test the threat history endpoint"""
    
    def test_history_returns_events_list(self):
        """GET /api/history returns threat events list"""
        response = requests.get(f"{BASE_URL}/api/history")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        
    def test_history_with_limit(self):
        """GET /api/history with limit parameter"""
        response = requests.get(f"{BASE_URL}/api/history?limit=10")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) <= 10


class TestCellRecordsEndpoint:
    """Test the cell records endpoint"""
    
    def test_cell_records_returns_list(self):
        """GET /api/cell-records returns cell tower records"""
        response = requests.get(f"{BASE_URL}/api/cell-records")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
    
    def test_cell_records_structure(self):
        """GET /api/cell-records returns proper record structure"""
        # First call dashboard to ensure we have data
        requests.get(f"{BASE_URL}/api/dashboard")
        
        response = requests.get(f"{BASE_URL}/api/cell-records?limit=5")
        assert response.status_code == 200
        data = response.json()
        
        if len(data) > 0:
            record = data[0]
            assert "lac" in record
            assert "tac" in record
            assert "cid" in record
            assert "network_type" in record


class TestSettingsEndpoints:
    """Test settings endpoints"""
    
    def test_get_settings(self):
        """GET /api/settings returns settings object"""
        response = requests.get(f"{BASE_URL}/api/settings")
        assert response.status_code == 200
        data = response.json()
        
        assert "notifications_enabled" in data
        assert "background_monitoring" in data
        assert "auto_reset_baseline" in data
        assert "scan_interval_seconds" in data
        
    def test_put_settings_updates_and_returns(self):
        """PUT /api/settings updates settings and returns updated settings"""
        # First get current settings
        get_response = requests.get(f"{BASE_URL}/api/settings")
        assert get_response.status_code == 200
        current = get_response.json()
        
        # Toggle a setting
        new_value = not current.get("notifications_enabled", True)
        update_payload = {
            "id": "default",
            "notifications_enabled": new_value,
            "background_monitoring": current.get("background_monitoring", True),
            "auto_reset_baseline": current.get("auto_reset_baseline", False),
            "scan_interval_seconds": current.get("scan_interval_seconds", 5),
            "threat_sensitivity": current.get("threat_sensitivity", "medium")
        }
        
        response = requests.put(
            f"{BASE_URL}/api/settings",
            json=update_payload,
            headers={"Content-Type": "application/json"}
        )
        assert response.status_code == 200
        data = response.json()
        assert data["notifications_enabled"] == new_value
        
        # Verify persistence
        verify_response = requests.get(f"{BASE_URL}/api/settings")
        assert verify_response.status_code == 200
        assert verify_response.json()["notifications_enabled"] == new_value


class TestSessionEndpoints:
    """Test session management endpoints"""
    
    def test_start_session(self):
        """POST /api/session/start starts a new monitoring session"""
        response = requests.post(f"{BASE_URL}/api/session/start")
        assert response.status_code == 200
        data = response.json()
        
        assert "id" in data
        assert "started_at" in data
        assert "is_active" in data
        assert data["is_active"] == True
        
    def test_stop_session(self):
        """POST /api/session/stop stops the session"""
        # First start a session
        requests.post(f"{BASE_URL}/api/session/start")
        
        # Stop it
        response = requests.post(f"{BASE_URL}/api/session/stop")
        assert response.status_code == 200
        data = response.json()
        
        assert "is_active" in data
        assert data["is_active"] == False


class TestDataManagement:
    """Test data management endpoints"""
    
    def test_clear_data(self):
        """DELETE /api/data clears all data"""
        # First add some data by calling dashboard
        requests.get(f"{BASE_URL}/api/dashboard")
        
        # Clear data
        response = requests.delete(f"{BASE_URL}/api/data")
        assert response.status_code == 200
        data = response.json()
        assert "message" in data
        
        # Verify data was cleared
        records_response = requests.get(f"{BASE_URL}/api/cell-records")
        assert records_response.status_code == 200
        records = records_response.json()
        assert len(records) == 0


class TestStatsEndpoint:
    """Test statistics endpoint"""
    
    def test_stats_returns_data(self):
        """GET /api/stats returns statistics"""
        response = requests.get(f"{BASE_URL}/api/stats")
        assert response.status_code == 200
        data = response.json()
        
        assert "total_scans" in data
        assert "total_threats" in data
        assert "high_severity_threats" in data
        assert "session_active" in data


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
