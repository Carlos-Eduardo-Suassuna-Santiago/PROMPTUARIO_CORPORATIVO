import importlib.util
from pathlib import Path
import sys

import pytest
from pydantic import ValidationError


def load_ai_schemas():
    path = Path(__file__).parents[3] / "backend" / "ai-service" / "app" / "domain" / "schemas.py"
    spec = importlib.util.spec_from_file_location("promptuario_ai_schemas", path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    module.DrugInteractionResponse.model_rebuild()
    module.SymptomAnalysisResponse.model_rebuild()
    module.ClinicalSummaryResponse.model_rebuild()
    return module


schemas = load_ai_schemas()


@pytest.mark.unit
def test_drug_interaction_schema_accepts_valid_response():
    result = schemas.validate_llm_response(
        {
            "risk_level": "HIGH",
            "interactions_found": [
                {"drugs": ["A", "B"], "severity": "HIGH", "description": "Interaction found"}
            ],
            "recommendations": ["Review treatment"],
        },
        "drug_interaction",
    )

    assert result["risk_level"] == "HIGH"
    assert result["interactions_found"][0]["drugs"] == ["A", "B"]


@pytest.mark.unit
def test_ai_schema_rejects_invalid_risk_and_unknown_schema():
    with pytest.raises(ValueError, match="LLM response validation failed"):
        schemas.validate_llm_response({"risk_level": "UNSAFE"}, "symptom_analysis")

    with pytest.raises(ValueError, match="Unknown response schema"):
        schemas.validate_llm_response({}, "not-supported")
