#!/usr/bin/env python3
"""Convert the device business opportunity Excel sheet into BOMS import JSON."""

from __future__ import annotations

import argparse
import json
import math
import re
from datetime import date, datetime
from pathlib import Path
from typing import Any

import pandas as pd


FIELD_INDEXES = {
    "submitDate": 0,
    "submitter": 1,
    "govMarketManager": 2,
    "submitterRegion": 3,
    "company": 4,
    "industry": 5,
    "name": 6,
    "supplierCompany": 7,
    "deviceTypes": 8,
    "deviceModels": 9,
    "demandQuantity": 10,
    "estimatedPurchaseAmount": 11,
    "canPrepareParams": 12,
    "winRateLabel": 13,
    "supplyRegion": 14,
    "requiresExclusiveAuthorization": 15,
    "authorizedCategories": 16,
    "expectedDeliveryDate": 17,
    "bidDeadline": 18,
    "bidWon": 19,
    "purchaseType": 20,
    "winningAmount": 21,
    "deliBgDeliveryAmount": 22,
    "salesDepartment": 23,
    "reportedSuccessfully": 24,
    "sales": 25,
    "creator": 26,
    "remindEditDate": 27,
    "lastEditDate": 28,
    "provinceGeneralManager": 29,
    "a4BusinessManager": 30,
    "a3BusinessManager": 31,
}

WIN_RATE_VALUES = {
    "10%-没有把握": 10,
    "30%-把握很小": 30,
    "50%-可以参与": 50,
    "70%-赢面很大": 70,
    "100%-肯定中标": 100,
}


def is_blank(value: Any) -> bool:
    if value is None:
        return True
    if isinstance(value, float) and math.isnan(value):
        return True
    return str(value).strip() == ""


def clean_text(value: Any) -> str | None:
    if is_blank(value):
        return None
    if isinstance(value, float) and value.is_integer():
        return str(int(value))
    return str(value).strip()


def parse_number(value: Any) -> float | None:
    text = clean_text(value)
    if not text:
        return None
    text = text.replace(",", "")
    match = re.search(r"-?\d+(?:\.\d+)?", text)
    if not match:
        return None
    return float(match.group(0))


def parse_int(value: Any) -> int | None:
    number = parse_number(value)
    if number is None:
        return None
    return int(round(number))


def parse_bool(value: Any) -> bool | None:
    text = clean_text(value)
    if text is None:
        return None
    if text in {"是", "yes", "Yes", "Y", "true", "True", "1"}:
        return True
    if text in {"否", "no", "No", "N", "false", "False", "0"}:
        return False
    return None


def normalize_date(value: Any) -> str | None:
    if is_blank(value):
        return None
    if isinstance(value, (datetime, date)):
        return value.strftime("%Y-%m-%d")
    text = str(value).strip()
    text = text.replace("年", "-").replace("月", "-").replace("日", "")
    text = text.replace("/", "-").replace(".", "-")
    match = re.search(r"(\d{4})-(\d{1,2})-(\d{1,2})", text)
    if not match:
        return text
    year, month, day = match.groups()
    return f"{int(year):04d}-{int(month):02d}-{int(day):02d}"


def normalize_wan_amount(value: Any) -> tuple[float | None, str | None]:
    """Return amount in ten-thousand yuan and how the source value was treated."""
    amount = parse_number(value)
    if amount is None:
        return None, None
    if amount > 1000:
        return round(amount / 10000, 4), "yuan"
    return amount, "wan"


def get_cell(row: pd.Series, field: str) -> Any:
    return row.iloc[FIELD_INDEXES[field]]


def build_opportunity(row: pd.Series, excel_row: int, source_file: str, sheet_name: str, headers: list[str]) -> dict[str, Any]:
    amount_wan, amount_source_unit = normalize_wan_amount(get_cell(row, "estimatedPurchaseAmount"))
    winning_wan, winning_source_unit = normalize_wan_amount(get_cell(row, "winningAmount"))
    delivery_wan, delivery_source_unit = normalize_wan_amount(get_cell(row, "deliBgDeliveryAmount"))

    bid_deadline = normalize_date(get_cell(row, "bidDeadline"))
    expected_delivery = normalize_date(get_cell(row, "expectedDeliveryDate"))
    win_rate_label = clean_text(get_cell(row, "winRateLabel"))
    sales = clean_text(get_cell(row, "sales"))
    submitter = clean_text(get_cell(row, "submitter"))
    creator = clean_text(get_cell(row, "creator"))
    owner = sales or submitter or creator or "历史Excel导入"

    raw_row = {
        headers[i] if i < len(headers) and headers[i] else f"column_{i + 1}": clean_text(row.iloc[i])
        for i in range(min(len(headers), len(row)))
        if not is_blank(row.iloc[i])
    }

    opportunity = {
        "name": clean_text(get_cell(row, "name")) or clean_text(get_cell(row, "company")) or f"历史商机-{excel_row}",
        "company": clean_text(get_cell(row, "company")),
        "stage": "prospecting",
        "value": round((amount_wan or 0) * 10000, 2),
        "probability": WIN_RATE_VALUES.get(win_rate_label, 20),
        "closeDate": bid_deadline or expected_delivery,
        "source": "历史Excel导入",
        "priority": "medium",
        "owner": owner,
        "submitDate": normalize_date(get_cell(row, "submitDate")),
        "submitter": submitter,
        "govMarketManager": clean_text(get_cell(row, "govMarketManager")),
        "submitterRegion": clean_text(get_cell(row, "submitterRegion")),
        "industry": clean_text(get_cell(row, "industry")),
        "supplierCompany": clean_text(get_cell(row, "supplierCompany")),
        "deviceTypes": clean_text(get_cell(row, "deviceTypes")),
        "deviceModels": clean_text(get_cell(row, "deviceModels")),
        "demandQuantity": parse_int(get_cell(row, "demandQuantity")),
        "estimatedPurchaseAmount": amount_wan,
        "estimatedPurchaseAmountUnit": "万元" if amount_wan is not None else None,
        "estimatedPurchaseAmountWan": amount_wan,
        "canPrepareParams": parse_bool(get_cell(row, "canPrepareParams")),
        "winRateLabel": win_rate_label,
        "supplyRegion": clean_text(get_cell(row, "supplyRegion")),
        "requiresExclusiveAuthorization": parse_bool(get_cell(row, "requiresExclusiveAuthorization")),
        "authorizedCategories": clean_text(get_cell(row, "authorizedCategories")),
        "expectedDeliveryDate": expected_delivery,
        "bidDeadline": bid_deadline,
        "bidWon": parse_bool(get_cell(row, "bidWon")),
        "purchaseType": clean_text(get_cell(row, "purchaseType")),
        "winningAmount": winning_wan,
        "winningAmountUnit": "万元" if winning_wan is not None else None,
        "winningAmountWan": winning_wan,
        "deliBgDeliveryAmount": delivery_wan,
        "deliBgDeliveryAmountUnit": "万元" if delivery_wan is not None else None,
        "deliBgDeliveryAmountWan": delivery_wan,
        "salesDepartment": clean_text(get_cell(row, "salesDepartment")),
        "reportedSuccessfully": parse_bool(get_cell(row, "reportedSuccessfully")),
        "sales": sales or owner,
        "creator": creator,
        "creatorName": creator,
        "submitterName": submitter,
        "ownerName": owner,
        "visibilityStatus": "UNASSIGNED",
        "lastEditor": creator or submitter or owner,
        "remindEditDate": normalize_date(get_cell(row, "remindEditDate")),
        "lastEditDate": normalize_date(get_cell(row, "lastEditDate")),
        "provinceGeneralManager": clean_text(get_cell(row, "provinceGeneralManager")),
        "a4BusinessManager": clean_text(get_cell(row, "a4BusinessManager")),
        "a3BusinessManager": clean_text(get_cell(row, "a3BusinessManager")),
        "businessProgressStatus": "新提报",
        "reminderStatus": "未提醒",
        "deviceRequirementVersion": 1,
        "deviceRequirementLockStatus": "UNLOCKED",
        "reportFlowStatus": "NOT_STARTED",
        "bidDocumentFlowStatus": "NOT_STARTED",
        "legacyExtraJson": json.dumps(
            {
                "importSource": "device-opportunity-excel",
                "sourceFile": source_file,
                "sheetName": sheet_name,
                "excelRow": excel_row,
                "amountRules": {
                    "estimatedPurchaseAmount": amount_source_unit,
                    "winningAmount": winning_source_unit,
                    "deliBgDeliveryAmount": delivery_source_unit,
                    "rule": "values greater than 1000 are treated as yuan and converted to 万元",
                },
                "rawRow": raw_row,
            },
            ensure_ascii=False,
        ),
        "tasks": [],
        "activities": [],
    }
    return {k: v for k, v in opportunity.items() if v is not None}


def convert(excel_path: Path) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    excel_file = pd.ExcelFile(excel_path)
    sheet_name = excel_file.sheet_names[0]
    df = pd.read_excel(excel_path, sheet_name=0, dtype=object).dropna(how="all")
    headers = [clean_text(c) or "" for c in df.columns]

    imported: list[dict[str, Any]] = []
    skipped: list[dict[str, Any]] = []
    amount_conversions: list[dict[str, Any]] = []

    for idx, row in df.iterrows():
        excel_row = int(idx + 2)
        has_core_value = any(
            not is_blank(get_cell(row, field))
            for field in ("company", "name", "deviceTypes")
        )
        if not has_core_value:
            skipped.append({"excelRow": excel_row, "reason": "missing company, name and device type"})
            continue

        opp = build_opportunity(row, excel_row, str(excel_path), sheet_name, headers)
        imported.append(opp)

        for field in ("estimatedPurchaseAmount", "winningAmount", "deliBgDeliveryAmount"):
            amount_wan, source_unit = normalize_wan_amount(get_cell(row, field))
            if source_unit == "yuan":
                amount_conversions.append(
                    {
                        "excelRow": excel_row,
                        "field": field,
                        "raw": parse_number(get_cell(row, field)),
                        "convertedWan": amount_wan,
                    }
                )

    report = {
        "source": str(excel_path),
        "sheet": sheet_name,
        "totalRows": int(len(df)),
        "importedRows": len(imported),
        "skippedRows": skipped,
        "amountConversionRule": "字段值 > 1000 时按元处理并除以 10000；字段值 <= 1000 时按万元处理。",
        "amountConversions": amount_conversions,
    }
    return imported, report


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("excel", type=Path)
    parser.add_argument("--json-out", type=Path, required=True)
    parser.add_argument("--report-out", type=Path, required=True)
    args = parser.parse_args()

    opportunities, report = convert(args.excel)
    args.json_out.parent.mkdir(parents=True, exist_ok=True)
    args.report_out.parent.mkdir(parents=True, exist_ok=True)
    args.json_out.write_text(json.dumps(opportunities, ensure_ascii=False, indent=2), encoding="utf-8")
    args.report_out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"json": str(args.json_out), "report": str(args.report_out), **report}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
