import importlib.util
from pathlib import Path
import sys

import pytest
from openpyxl import load_workbook


def load_module(relative_path: str, name: str):
    path = Path(__file__).parents[3] / "backend" / relative_path
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    if name == "promptuario_reporting_schemas":
        module.MultiSheetExportRequest.model_rebuild()
        module.CustomReportRequest.model_rebuild()
    return module


schemas = load_module("reporting-service/app/schemas/report.py", "promptuario_reporting_schemas")
xlsx = load_module("reporting-service/app/infrastructure/xlsx_builder.py", "promptuario_reporting_xlsx")


@pytest.mark.unit
def test_custom_report_rejects_nested_or_oversized_parameters():
    with pytest.raises(ValueError):
        schemas.CustomReportRequest(
            sql_query_name="patients",
            parameters={"nested": {"unsafe": True}},
        )

    with pytest.raises(ValueError):
        schemas.CustomReportRequest(
            sql_query_name="patients",
            parameters={"name": "x" * 501},
        )


@pytest.mark.unit
def test_multisheet_request_requires_xlsx_filename():
    request = schemas.MultiSheetExportRequest(
        filename="patients.xlsx",
        sheets=[{"sheet_name": "Patients", "report_type": "PATIENTS"}],
    )

    assert request.sheets[0].sheet_name == "Patients"

    with pytest.raises(ValueError, match="end with .xlsx"):
        schemas.MultiSheetExportRequest(
            filename="patients.csv",
            sheets=[{"sheet_name": "Patients", "report_type": "PATIENTS"}],
        )


@pytest.mark.unit
def test_xlsx_worker_builds_single_and_multi_sheet_workbooks():
    single = load_workbook(__import__("io").BytesIO(xlsx.build_xlsx([{"id": 1, "name": "Maria"}])))
    multi = load_workbook(__import__("io").BytesIO(xlsx.build_multi_sheet_xlsx([
        {"sheet_name": "Patients", "data": [{"id": 1}]},
        {"sheet_name": "Empty", "data": []},
    ])))

    assert single.active.title == "Report"
    assert single.active["A1"].value == "id"
    assert single.active["B2"].value == "Maria"
    assert multi.sheetnames == ["Patients", "Empty"]
    assert multi["Empty"]["A1"].value == "No data available"
