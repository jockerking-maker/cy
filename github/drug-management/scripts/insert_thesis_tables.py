# -*- coding: utf-8 -*-
"""Insert database table definitions into thesis section 4.3.2 via Word COM."""
import os
import re
import sys
import glob

# Table definitions: (caption, table_name, rows as list of (field, type, nullable, desc))
# Nullable: "NOT NULL" or "NULL" or "DEFAULT x"
TABLES_AFTER_4_4 = [
    (
        "表4-5  采购订单明细表（purchase_order_item）",
        [
            ("item_id", "BIGINT", "NOT NULL", "明细ID，主键，自增"),
            ("order_id", "BIGINT", "NOT NULL", "订单ID，外键"),
            ("drug_id", "BIGINT", "NOT NULL", "药品ID，外键"),
            ("purchase_num", "INT", "NOT NULL", "采购数量"),
            ("purchase_price", "DECIMAL(10,2)", "NOT NULL", "采购单价"),
            ("amount", "DECIMAL(12,2)", "NOT NULL", "小计金额"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
        ],
    ),
    (
        "表4-6  供应商信息表（supplier_info）",
        [
            ("supplier_id", "BIGINT", "NOT NULL", "供应商ID，主键，自增"),
            ("supplier_code", "VARCHAR(50)", "NOT NULL", "供应商编码，唯一"),
            ("supplier_name", "VARCHAR(100)", "NOT NULL", "供应商名称"),
            ("contact_person", "VARCHAR(50)", "NULL", "联系人"),
            ("contact_phone", "VARCHAR(20)", "NULL", "联系电话"),
            ("address", "VARCHAR(200)", "NULL", "地址"),
            ("license_no", "VARCHAR(100)", "NULL", "营业执照号"),
            ("status", "INT", "NULL", "状态（0停用/1启用）"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("update_time", "DATETIME", "NULL", "更新时间"),
            ("is_deleted", "TINYINT", "NOT NULL", "逻辑删除（0未删/1已删）"),
        ],
    ),
    (
        "表4-7  仓库信息表（warehouse_info）",
        [
            ("warehouse_id", "BIGINT", "NOT NULL", "仓库ID，主键，自增"),
            ("warehouse_name", "VARCHAR(100)", "NOT NULL", "仓库名称"),
            ("warehouse_code", "VARCHAR(50)", "NOT NULL", "仓库编码，唯一"),
            ("address", "VARCHAR(200)", "NULL", "仓库地址"),
            ("manager", "VARCHAR(50)", "NULL", "管理员"),
            ("phone", "VARCHAR(20)", "NULL", "联系电话"),
            ("status", "INT", "NULL", "状态（0停用/1启用）"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("update_time", "DATETIME", "NULL", "更新时间"),
            ("is_deleted", "TINYINT", "NOT NULL", "逻辑删除（0未删/1已删）"),
        ],
    ),
    (
        "表4-8  药品入库表（drug_in）",
        [
            ("in_id", "BIGINT", "NOT NULL", "入库ID，主键，自增"),
            ("in_no", "VARCHAR(50)", "NOT NULL", "入库单号，唯一"),
            ("order_id", "BIGINT", "NULL", "采购订单ID，外键"),
            ("in_type", "VARCHAR(20)", "NULL", "入库类型（采购入库/退货入库等）"),
            ("drug_id", "BIGINT", "NOT NULL", "药品ID，外键"),
            ("warehouse_id", "BIGINT", "NOT NULL", "仓库ID，外键"),
            ("batch_no", "VARCHAR(50)", "NULL", "批号"),
            ("quantity", "INT", "NOT NULL", "入库数量"),
            ("purchase_price", "DECIMAL(10,2)", "NOT NULL", "入库单价"),
            ("production_date", "DATE", "NULL", "生产日期"),
            ("expiry_date", "DATE", "NULL", "有效期"),
            ("in_date", "DATETIME", "NULL", "入库时间"),
            ("create_user_id", "BIGINT", "NULL", "操作人ID"),
            ("remark", "VARCHAR(500)", "NULL", "备注"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("is_deleted", "TINYINT", "NOT NULL", "逻辑删除（0未删/1已删）"),
        ],
    ),
    (
        "表4-9  药品出库表（drug_out）",
        [
            ("out_id", "BIGINT", "NOT NULL", "出库ID，主键，自增"),
            ("out_no", "VARCHAR(50)", "NOT NULL", "出库单号，唯一"),
            ("drug_id", "BIGINT", "NOT NULL", "药品ID，外键"),
            ("warehouse_id", "BIGINT", "NOT NULL", "仓库ID，外键"),
            ("batch_no", "VARCHAR(50)", "NULL", "批号"),
            ("quantity", "INT", "NOT NULL", "出库数量"),
            ("sale_price", "DECIMAL(10,2)", "NOT NULL", "出库单价"),
            ("out_type", "VARCHAR(20)", "NULL", "出库类型（领用/销售/报损）"),
            ("relate_no", "VARCHAR(50)", "NULL", "关联单号"),
            ("out_date", "DATETIME", "NULL", "出库时间"),
            ("create_user_id", "BIGINT", "NULL", "操作人ID"),
            ("remark", "VARCHAR(500)", "NULL", "备注"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("is_deleted", "TINYINT", "NOT NULL", "逻辑删除（0未删/1已删）"),
        ],
    ),
    (
        "表4-10  药品锁定记录表（drug_lock）",
        [
            ("lock_id", "BIGINT", "NOT NULL", "锁定ID，主键，自增"),
            ("lock_no", "VARCHAR(50)", "NOT NULL", "锁定单号，唯一"),
            ("drug_id", "BIGINT", "NOT NULL", "药品ID，外键"),
            ("warehouse_id", "BIGINT", "NOT NULL", "仓库ID，外键"),
            ("batch_no", "VARCHAR(50)", "NULL", "批次号"),
            ("lock_num", "INT", "NOT NULL", "锁定数量"),
            ("unlock_num", "INT", "NOT NULL", "已解锁数量"),
            ("lock_reason", "VARCHAR(500)", "NULL", "锁定原因"),
            ("lock_user_id", "BIGINT", "NULL", "锁定人ID"),
            ("lock_time", "DATETIME", "NULL", "锁定时间"),
            ("unlock_user_id", "BIGINT", "NULL", "解锁人ID"),
            ("unlock_time", "DATETIME", "NULL", "解锁时间"),
            ("status", "INT", "NULL", "状态（0锁定中/1已解锁/2已取消）"),
            ("remark", "VARCHAR(500)", "NULL", "备注"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("update_time", "DATETIME", "NULL", "更新时间"),
            ("is_deleted", "TINYINT", "NOT NULL", "逻辑删除（0未删/1已删）"),
        ],
    ),
    (
        "表4-11  库存盘点表（stock_check）",
        [
            ("check_id", "BIGINT", "NOT NULL", "盘点ID，主键，自增"),
            ("check_no", "VARCHAR(50)", "NOT NULL", "盘点单号，唯一"),
            ("warehouse_id", "BIGINT", "NOT NULL", "仓库ID，外键"),
            ("check_time", "DATETIME", "NULL", "盘点日期"),
            ("status", "INT", "NULL", "状态（0盘点中/1已完成/2已取消）"),
            ("remark", "VARCHAR(500)", "NULL", "备注"),
            ("check_user_id", "BIGINT", "NULL", "盘点人ID"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("update_time", "DATETIME", "NULL", "更新时间"),
        ],
    ),
    (
        "表4-12  库存盘点明细表（stock_check_item）",
        [
            ("item_id", "BIGINT", "NOT NULL", "明细ID，主键，自增"),
            ("check_id", "BIGINT", "NOT NULL", "盘点ID，外键"),
            ("drug_id", "BIGINT", "NOT NULL", "药品ID，外键"),
            ("batch_no", "VARCHAR(50)", "NULL", "批号"),
            ("system_num", "INT", "NULL", "系统数量"),
            ("actual_num", "INT", "NULL", "实际数量"),
            ("diff_num", "INT", "NULL", "差异数量"),
            ("handle_way", "VARCHAR(100)", "NULL", "处理方式"),
            ("handle_remark", "VARCHAR(500)", "NULL", "处理备注"),
            ("remark", "VARCHAR(500)", "NULL", "备注"),
        ],
    ),
    (
        "表4-13  库存预警记录表（stock_warning）",
        [
            ("warning_id", "BIGINT", "NOT NULL", "预警ID，主键，自增"),
            ("warning_no", "VARCHAR(50)", "NOT NULL", "预警单号，唯一"),
            ("drug_id", "BIGINT", "NOT NULL", "药品ID，外键"),
            ("warehouse_id", "BIGINT", "NOT NULL", "仓库ID，外键"),
            ("batch_no", "VARCHAR(50)", "NULL", "批号"),
            ("stock_num", "INT", "NOT NULL", "当前库存"),
            ("warning_type", "INT", "NOT NULL", "预警类型（0低于最低/1高于最高）"),
            ("warning_type_name", "VARCHAR(50)", "NULL", "预警类型名称"),
            ("min_warning_num", "INT", "NULL", "最低预警值"),
            ("max_warning_num", "INT", "NULL", "最高预警值"),
            ("warning_level", "INT", "NULL", "预警级别（0一般/1重要/2紧急）"),
            ("handle_status", "INT", "NULL", "处理状态（0未处理/1已处理）"),
            ("handle_user_id", "BIGINT", "NULL", "处理人ID"),
            ("assign_user_id", "BIGINT", "NULL", "指派处理人ID"),
            ("handle_time", "DATETIME", "NULL", "处理时间"),
            ("handle_remark", "VARCHAR(500)", "NULL", "处理备注"),
            ("suggestion", "VARCHAR(500)", "NULL", "处理建议"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("update_time", "DATETIME", "NULL", "更新时间"),
            ("is_deleted", "TINYINT", "NOT NULL", "逻辑删除（0未删/1已删）"),
        ],
    ),
    (
        "表4-14  审核记录表（audit_record）",
        [
            ("record_id", "BIGINT", "NOT NULL", "记录ID，主键，自增"),
            ("order_id", "BIGINT", "NOT NULL", "采购订单ID，外键"),
            ("audit_user_id", "BIGINT", "NULL", "审核人ID"),
            ("audit_user_name", "VARCHAR(50)", "NULL", "审核人姓名"),
            ("audit_result", "INT", "NULL", "审核结果（1通过/2驳回）"),
            ("audit_remark", "VARCHAR(500)", "NULL", "审核意见"),
            ("audit_time", "DATETIME", "NULL", "审核时间"),
            ("audit_level", "INT", "NULL", "审核级别（1一级/2二级/3三级）"),
        ],
    ),
    (
        "表4-15  系统角色表（sys_role）",
        [
            ("role_id", "BIGINT", "NOT NULL", "角色ID，主键，自增"),
            ("role_name", "VARCHAR(50)", "NOT NULL", "角色名称"),
            ("role_code", "VARCHAR(50)", "NOT NULL", "角色编码，唯一"),
            ("description", "VARCHAR(200)", "NULL", "角色描述"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("update_time", "DATETIME", "NULL", "更新时间"),
            ("is_deleted", "TINYINT", "NOT NULL", "逻辑删除（0未删/1已删）"),
        ],
    ),
    (
        "表4-16  系统菜单表（sys_menu）",
        [
            ("menu_id", "BIGINT", "NOT NULL", "菜单ID，主键，自增"),
            ("menu_name", "VARCHAR(50)", "NOT NULL", "菜单名称"),
            ("parent_id", "BIGINT", "NULL", "父菜单ID"),
            ("path", "VARCHAR(100)", "NULL", "菜单路径"),
            ("component", "VARCHAR(100)", "NULL", "组件路径"),
            ("icon", "VARCHAR(50)", "NULL", "菜单图标"),
            ("sort", "INT", "NULL", "排序"),
            ("status", "INT", "NULL", "状态（0禁用/1启用）"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("update_time", "DATETIME", "NULL", "更新时间"),
        ],
    ),
    (
        "表4-17  角色菜单关联表（sys_role_menu）",
        [
            ("id", "BIGINT", "NOT NULL", "ID，主键，自增"),
            ("role_id", "BIGINT", "NOT NULL", "角色ID，外键"),
            ("menu_id", "BIGINT", "NOT NULL", "菜单ID，外键"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
        ],
    ),
    (
        "表4-18  系统公告表（system_notice）",
        [
            ("notice_id", "BIGINT", "NOT NULL", "公告ID，主键，自增"),
            ("title", "VARCHAR(200)", "NOT NULL", "公告标题"),
            ("content", "TEXT", "NOT NULL", "公告内容"),
            ("create_user_id", "BIGINT", "NULL", "创建人ID"),
            ("create_user_name", "VARCHAR(50)", "NULL", "创建人姓名"),
            ("status", "INT", "NULL", "状态（0停用/1启用）"),
            ("create_time", "DATETIME", "NULL", "创建时间"),
            ("update_time", "DATETIME", "NULL", "更新时间"),
            ("is_deleted", "TINYINT", "NOT NULL", "逻辑删除（0未删/1已删）"),
        ],
    ),
    (
        "表4-19  操作日志表（sys_operation_log）",
        [
            ("log_id", "BIGINT", "NOT NULL", "日志ID，主键，自增"),
            ("user_id", "BIGINT", "NULL", "操作人ID"),
            ("user_name", "VARCHAR(50)", "NULL", "操作人姓名"),
            ("operation", "VARCHAR(100)", "NULL", "操作类型"),
            ("module", "VARCHAR(50)", "NULL", "操作模块"),
            ("description", "VARCHAR(500)", "NULL", "操作描述"),
            ("method", "VARCHAR(200)", "NULL", "请求方法"),
            ("params", "TEXT", "NULL", "请求参数"),
            ("ip", "VARCHAR(50)", "NULL", "操作IP"),
            ("create_time", "DATETIME", "NULL", "操作时间"),
        ],
    ),
]

PHYSICAL_INDEX_TEXT = """为提高数据库查询性能，本系统在以下字段上建立了索引：
(1) drug_info表：在drug_code字段上建立唯一索引，在supplier_id字段上建立普通索引，加速药品编码查询及按供应商筛选。
(2) drug_stock表：在(drug_id, warehouse_id, batch_no)上建立唯一联合索引，在drug_id、warehouse_id字段上分别建立索引，加速按药品、仓库及批次维度的库存查询。
(3) purchase_order表：在order_no字段上建立唯一索引，在supplier_id字段上建立索引，加速订单编号校验及按供应商筛选。
(4) purchase_order_item表：在order_id、drug_id字段上建立索引，加速订单明细关联查询。
(5) drug_in、drug_out表：在drug_id、warehouse_id字段上建立索引，加速出入库记录查询。
(6) stock_warning表：在warning_no字段上建立唯一索引，在drug_id、warehouse_id、handle_status字段上建立索引，加速预警记录查询和筛选。
(7) audit_record表：在order_id字段上建立索引，加速采购单审核历史查询。
(8) sys_user表：在username字段上建立唯一索引，加速登录校验。
(9) sys_operation_log表：在user_id、module、create_time字段上建立索引，支持操作日志审计检索。"""


def find_thesis_doc():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    candidates = []
    for pattern in ("*2026*.doc", "*2026*.docx"):
        for path in glob.glob(os.path.join(root, pattern)):
            base = os.path.basename(path)
            if base.startswith("~$"):
                continue
            candidates.append(path)
    if not candidates:
        raise FileNotFoundError("Thesis doc not found under " + root)
    # Prefer .doc over .docx; largest file is usually the full thesis
    candidates.sort(key=lambda p: (0 if p.lower().endswith(".doc") else 1, -os.path.getsize(p)))
    return candidates[0]


def add_word_table(doc, caption, rows, style_table="Table Grid"):
    """Append caption + 4-column table at end of document (for temp doc assembly)."""
    end = doc.Content.End - 1
    rng = doc.Range(end, end)
    rng.InsertAfter(caption + "\r")
    cap_end = doc.Content.End - 1
    tbl_rng = doc.Range(cap_end, cap_end)
    nrows = len(rows) + 1
    tbl = doc.Tables.Add(tbl_rng, nrows, 4)
    try:
        tbl.Style = style_table
    except Exception:
        try:
            tbl.Style = "网格型"
        except Exception:
            pass
    headers = ("字段名", "数据类型", "是否为空", "说明")
    for c, h in enumerate(headers, 1):
        cell = tbl.Cell(1, c)
        cell.Range.Text = h
        cell.Range.Font.Bold = True
    for r, row in enumerate(rows, 2):
        for c, val in enumerate(row, 1):
            tbl.Cell(r, c).Range.Text = val
    tbl.Range.Font.Name = "宋体"
    tbl.Range.Font.Size = 10.5
    doc.Range(doc.Content.End - 1, doc.Content.End - 1).InsertAfter("\r")


def main():
    doc_path = find_thesis_doc()
    print("Thesis file:", doc_path)

    try:
        import win32com.client
    except ImportError:
        print("pywin32 not installed; generating markdown supplement only.", file=sys.stderr)
        write_markdown_supplement(doc_path)
        return 1

    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False
    word.DisplayAlerts = 0

    doc = word.Documents.Open(os.path.abspath(doc_path))
    find = doc.Content.Find
    find.ClearFormatting()
    find.Replacement.ClearFormatting()

    # Update table count text
    find.Text = "本系统共设计了15张数据表"
    if find.Execute(Replace=2):  # wdReplaceOne
        find.Text = "本系统共设计了15张数据表"
        find.Replacement.Text = "本系统共设计了19张数据表"
        find.Execute(Replace=2)

    # Renumber API table 4-5 -> 4-20 in section 4.4 (do before inserting new 4-5..4-19)
    for old, new in [
        ("表4-5  系统主要API接口", "表4-20  系统主要API接口"),
    ]:
        find.Text = old
        find.Replacement.Text = new
        find.Execute(Replace=2)  # replace all - need loop
    find.Text = "表4-5  系统主要API接口"
    find.Replacement.Text = "表4-20  系统主要API接口"
    while find.Execute(Replace=2):
        pass

    # Find insertion point: before section 4.3.3
    insert_range = None
    for marker in ("4.3.3 物理结构设计", "4.3.3"):
        find.Text = marker
        find.MatchWildcards = False
        find.Forward = True
        find.Wrap = 0  # wdFindStop
        if find.Execute():
            insert_range = find.Parent
            break
    if insert_range is None:
        plain = doc.Content.Text
        pos = plain.find("4.3.3")
        if pos < 0:
            raise RuntimeError("Cannot find section 4.3.3")
        insert_range = doc.Range(pos, pos)
    # Skip if tables already inserted
    if "表4-5  采购订单明细表" not in doc.Content.Text:
        temp = word.Documents.Add()
        for caption, rows in TABLES_AFTER_4_4:
            add_word_table(temp, caption, rows)
        temp.Content.Copy()
        insert_range.Select()
        word.Selection.Paste()
        temp.Close(False)
    else:
        print("Tables already present, skipping paste.")

    # Replace 4.3.3 body (re-find after paste; character offsets are invalid)
    find.Text = "4.3.3 物理结构设计"
    if not find.Execute():
        find.Text = "4.3.3"
        find.Execute()
    start = find.Parent.End
    find.Text = "4.4 接口设计"
    if not find.Execute():
        find.Text = "4.4"
        find.Execute()
    end = find.Parent.Start
    if end > start:
        rng = doc.Range(Start=start, End=end)
        rng.Text = "\r\n" + PHYSICAL_INDEX_TEXT + "\r\n\r\n"

    # Update summary in chapter 7 if present
    find.Text = "设计了10个功能模块和15张数据表"
    find.Replacement.Text = "设计了10个功能模块和19张数据表"
    while find.Execute(Replace=2):
        pass

    out_path = doc_path
    if doc_path.lower().endswith(".doc"):
        out_docx = doc_path[:-4] + "_updated.docx"
        doc.SaveAs2(out_docx, FileFormat=16)  # wdFormatXMLDocument
        print("Saved:", out_docx)
        doc.Save()
    else:
        doc.Save()
        print("Saved:", out_path)

    doc.Close()
    word.Quit()
    print("Done.")
    return 0


def write_markdown_supplement(doc_path):
    out = os.path.join(os.path.dirname(doc_path), "论文数据库表补充_4.3.2.md")
    lines = [
        "# 4.3.2 逻辑结构设计 — 数据库表补充（插入于表4-4之后、4.3.3之前）\n",
        "请将下文「本系统共设计了15张数据表」改为「19张数据表」。",
        "请将 4.4 节「表4-5 系统主要API接口」改为「表4-20」。\n",
    ]
    for caption, rows in TABLES_AFTER_4_4:
        lines.append(f"\n## {caption}\n\n")
        lines.append("| 字段名 | 数据类型 | 是否为空 | 说明 |\n")
        lines.append("| --- | --- | --- | --- |\n")
        for row in rows:
            lines.append(f"| {' | '.join(row)} |\n")
    lines.append("\n## 4.3.3 物理结构设计（替换原文）\n\n")
    lines.append(PHYSICAL_INDEX_TEXT + "\n")
    with open(out, "w", encoding="utf-8") as f:
        f.writelines(lines)
    print("Wrote:", out)


if __name__ == "__main__":
    sys.exit(main() or 0)
