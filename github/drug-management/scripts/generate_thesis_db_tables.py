# -*- coding: utf-8 -*-
"""Generate thesis database table markdown from hospital_drug.sql."""

import re
from pathlib import Path

SQL_PATH = Path(__file__).resolve().parent.parent / "hospital_drug.sql"
OUT_PATH = Path(__file__).resolve().parent.parent / "论文数据库表_4.3.2.md"

TABLE_ORDER = [
    ("drug_info", "表4-1", "药品信息表"),
    ("purchase_order", "表4-2", "采购订单表"),
    ("drug_stock", "表4-3", "药品库存表"),
    ("sys_user", "表4-4", "系统用户表"),
    ("purchase_order_item", "表4-5", "采购订单明细表"),
    ("supplier_info", "表4-6", "供应商信息表"),
    ("warehouse_info", "表4-7", "仓库信息表"),
    ("drug_in", "表4-8", "药品入库表"),
    ("drug_out", "表4-9", "药品出库表"),
    ("drug_lock", "表4-10", "药品锁定记录表"),
    ("stock_check", "表4-11", "库存盘点表"),
    ("stock_check_item", "表4-12", "库存盘点明细表"),
    ("stock_warning", "表4-13", "库存预警记录表"),
    ("audit_record", "表4-14", "审核记录表"),
    ("sys_role", "表4-15", "系统角色表"),
    ("sys_menu", "表4-16", "系统菜单表"),
    ("sys_role_menu", "表4-17", "角色菜单关联表"),
    ("system_notice", "表4-18", "系统公告表"),
    ("sys_operation_log", "表4-19", "操作日志表"),
]


def normalize_length(base_type: str, raw_type: str) -> str:
    match = re.search(r"\(([^)]+)\)", raw_type)
    if not match:
        return "-"
    inner = match.group(1).replace(" ", "")
    if base_type in {"bigint", "int", "tinyint", "datetime"} and inner == "0":
        return "-"
    return inner


def parse_default(rest: str, is_auto_increment: bool) -> str:
    if is_auto_increment:
        return "自增"
    match = re.search(
        r"DEFAULT\s+((?:CURRENT_TIMESTAMP(?:\([^)]*\))?)|'(?:[^']*)'|\d+(?:\.\d+)?)",
        rest,
        re.I,
    )
    if not match:
        return ""
    value = match.group(1)
    return value.replace("CURRENT_TIMESTAMP(0)", "CURRENT_TIMESTAMP")


def parse_column_line(line: str) -> dict | None:
    line = line.strip().rstrip(",")
    if not line.startswith("`"):
        return None

    comment = ""
    comment_match = re.search(r"COMMENT\s+'([^']*)'", line)
    if comment_match:
        comment = comment_match.group(1).strip()
        line = line[: comment_match.start()].strip()

    col_match = re.match(r"`([^`]+)`\s+(.+)", line)
    if not col_match:
        return None

    col_name = col_match.group(1)
    remainder = col_match.group(2)

    type_match = re.match(
        r"([a-z]+(?:\s*\([^)]+\))?)",
        remainder,
        re.I,
    )
    if not type_match:
        return None

    raw_type = re.sub(r"\s+", "", type_match.group(1).lower())
    base_type = re.match(r"([a-z]+)", raw_type).group(1)
    rest = remainder[type_match.end() :]

    is_auto_increment = "AUTO_INCREMENT" in rest.upper()
    default = parse_default(rest, is_auto_increment)

    return {
        "name": col_name,
        "type": base_type,
        "length": normalize_length(base_type, raw_type),
        "comment": comment,
        "default": default,
    }


def parse_tables(sql_text: str) -> dict:
    tables = {}
    pattern = re.compile(
        r"CREATE TABLE `(?P<name>[^`]+)`\s*\((?P<body>.*?)\)\s*ENGINE",
        re.S,
    )
    for match in pattern.finditer(sql_text):
        name = match.group("name")
        body = match.group("body")

        pk_cols = set()
        pk_match = re.search(r"PRIMARY KEY \(`([^`]+)`\)", body)
        if pk_match:
            pk_cols.add(pk_match.group(1))

        cols = []
        for line in body.splitlines():
            parsed = parse_column_line(line)
            if not parsed:
                continue
            parsed["pk"] = "主键" if parsed["name"] in pk_cols else "否"
            cols.append(parsed)
        tables[name] = cols
    return tables


def render_table(cols: list) -> str:
    lines = [
        "| 字段名称 | 类型 | 长度 | 字段说明 | 主键 | 默认值 |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for col in cols:
        lines.append(
            f"| {col['name']} | {col['type']} | {col['length']} | {col['comment']} | {col['pk']} | {col['default']} |"
        )
    return "\n".join(lines)


def main():
    sql_text = SQL_PATH.read_text(encoding="utf-8")
    tables = parse_tables(sql_text)

    parts = [
        "# 4.3.2 逻辑结构设计 — 数据库表",
        "",
        "请将下文「本系统共设计了15张数据表」改为「19张数据表」。请将 4.4 节「表4-5 系统主要API接口」改为「表4-20」。",
        "",
        "本系统共设计了19张数据表，核心数据表结构如下。",
        "",
    ]

    for table_name, table_no, table_title in TABLE_ORDER:
        cols = tables.get(table_name)
        if not cols:
            raise KeyError(f"Table not found in SQL: {table_name}")
        parts.append(f"{table_no}  {table_title}（{table_name}）")
        parts.append("")
        parts.append(render_table(cols))
        parts.append("")

    parts.extend(
        [
            "## 4.3.3 物理结构设计（替换原文）",
            "",
            "为提高数据库查询性能，本系统在以下字段上建立了索引：",
            "(1) drug_info表：在drug_code字段上建立唯一索引，在supplier_id字段上建立普通索引，加速药品编码查询及按供应商筛选。",
            "(2) drug_stock表：在(drug_id, warehouse_id, batch_no)上建立唯一联合索引，在drug_id、warehouse_id字段上分别建立索引，加速按药品、仓库及批次维度的库存查询。",
            "(3) purchase_order表：在order_no字段上建立唯一索引，在supplier_id字段上建立索引，加速订单编号校验及按供应商筛选。",
            "(4) purchase_order_item表：在order_id、drug_id字段上建立索引，加速订单明细关联查询。",
            "(5) drug_in、drug_out表：在drug_id、warehouse_id字段上建立索引，加速出入库记录查询。",
            "(6) stock_warning表：在warning_no字段上建立唯一索引，在drug_id、warehouse_id、handle_status字段上建立索引，加速预警记录查询和筛选。",
            "(7) audit_record表：在order_id字段上建立索引，加速采购单审核历史查询。",
            "(8) sys_user表：在username字段上建立唯一索引，加速登录校验。",
            "(9) sys_operation_log表：在user_id、module、create_time字段上建立索引，支持操作日志审计检索。",
        ]
    )

    OUT_PATH.write_text("\n".join(parts), encoding="utf-8")
    print(f"Generated: {OUT_PATH}")


if __name__ == "__main__":
    main()
