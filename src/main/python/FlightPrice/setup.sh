#!/usr/bin/env bash
set -euo pipefail

echo "=== FlightPrice Setup ==="

# Check Python 3
if ! command -v python3 &>/dev/null; then
    echo "ERROR: python3 is required. Install it first."
    exit 1
fi

# Install Python dependencies
echo "→ Installing Python packages…"
pip3 install -r "$(dirname "$0")/requirements.txt"

# Install Playwright browser
echo "→ Installing Chromium for Playwright…"
python3 -m playwright install chromium

echo ""
echo "=== Setup complete ==="
echo "Usage:"
echo "  python3 $(dirname "$0")/flight_price_scanner.py --from SEA --to PVG"
echo "  python3 $(dirname "$0")/flight_price_scanner.py --from SEA --to PVG --days 14"
echo "  python3 $(dirname "$0")/flight_price_scanner.py --from LAX --to NRT --no-headless"
