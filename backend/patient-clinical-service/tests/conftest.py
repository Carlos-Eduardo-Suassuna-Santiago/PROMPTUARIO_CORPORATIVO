"""
Pytest bootstrap for the unified Patient-Clinical service tests.

Ensures both the service root (``app`` package) and the backend root
(``shared`` package) are importable, with service root FIRST so that
``import app`` resolves to the unified service and not to a legacy
``patient-service/app`` or ``clinical-service/app`` shadow.
"""
from __future__ import annotations

import sys
from pathlib import Path

SERVICE_ROOT = Path(__file__).resolve().parents[1]
BACKEND_ROOT = SERVICE_ROOT.parent

for _path in (BACKEND_ROOT, SERVICE_ROOT):
    if str(_path) in sys.path:
        sys.path.remove(str(_path))
    sys.path.insert(0, str(_path))

