#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
医院药品管理系统 — Chen 记法 E-R 图生成脚本（draw.io 格式，精简版）

用法:
  python scripts/generate_chen_er_diagram.py

输出:
  drawio/系统ER图-Chen记法.drawio
"""

from __future__ import annotations

import html
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "drawio" / "系统ER图-Chen记法.drawio"

PAGE_W, PAGE_H = 1100, 720
EW, EH = 96, 42
RW, RH = 72, 52

# 精简：10 个核心实体，分层排布，减少交叉
ENTITIES: list[tuple[str, str, int, int]] = [
    ("e_supplier", "供应商", 80, 260),
    ("e_drug", "药品", 320, 260),
    ("e_warehouse", "仓库", 80, 120),
    ("e_stock", "药品库存", 320, 120),
    ("e_po", "采购订单", 720, 180),
    ("e_poi", "采购明细", 720, 300),
    ("e_in", "入库记录", 720, 60),
    ("e_out", "出库记录", 720, 420),
    ("e_user", "用户", 280, 520),
    ("e_role", "角色", 480, 520),
]

RELATIONS: list[tuple[str, str]] = [
    ("r_supply", "供应"),
    ("r_store", "存储"),
    ("r_keep", "存放"),
    ("r_order", "下单"),
    ("r_contain", "包含"),
    ("r_purchase", "采购"),
    ("r_in_po", "关联"),
    ("r_in_drug", "入库"),
    ("r_outbound", "出库"),
    ("r_create", "创建"),
    ("r_belong", "属于"),
]

LINKS: list[tuple[str, str, str, str, str]] = [
    ("e_supplier", "r_supply", "e_drug", "1", "N"),
    ("e_drug", "r_store", "e_stock", "1", "N"),
    ("e_warehouse", "r_keep", "e_stock", "1", "N"),
    ("e_supplier", "r_order", "e_po", "1", "N"),
    ("e_po", "r_contain", "e_poi", "1", "N"),
    ("e_poi", "r_purchase", "e_drug", "N", "1"),
    ("e_po", "r_in_po", "e_in", "1", "N"),
    ("e_drug", "r_in_drug", "e_in", "1", "N"),
    ("e_drug", "r_outbound", "e_out", "1", "N"),
    ("e_user", "r_create", "e_po", "1", "N"),
    ("e_user", "r_belong", "e_role", "N", "1"),
]

ZONES: list[tuple[str, int, int, int, int]] = []

RELATION_OVERRIDES: dict[str, tuple[int, int]] = {
    "r_order": (400, 330),
    "r_create": (500, 400),
}

EDGE_WAYPOINTS: dict[tuple[str, str, str], tuple[list[tuple[int, int]], list[tuple[int, int]]]] = {
    ("e_supplier", "r_order", "e_po"): ([(168, 400)], [(720, 400)]),
    ("e_user", "r_create", "e_po"): ([(368, 460)], [(720, 460)]),
}

RECT_STYLE = (
    "whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#000000;"
    "fontColor=#000000;fontSize=14;align=center;verticalAlign=middle;"
)
DIAMOND_STYLE = (
    "rhombus;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#000000;"
    "fontColor=#000000;fontSize=13;align=center;verticalAlign=middle;"
)
EDGE_STYLE = "endArrow=none;endFill=0;strokeColor=#000000;strokeWidth=1.5;html=1;rounded=0;"
CARD_STYLE = (
    "text;html=1;strokeColor=none;fillColor=none;align=center;"
    "verticalAlign=middle;fontSize=12;fontStyle=1;"
)
TITLE_STYLE = (
    "text;html=1;strokeColor=none;fillColor=none;align=center;"
    "verticalAlign=middle;fontSize=20;fontStyle=1;"
)
LEGEND_STYLE = (
    "text;html=1;strokeColor=none;fillColor=none;align=left;"
    "verticalAlign=top;fontSize=12;"
)
ZONE_STYLE = (
    "rounded=1;whiteSpace=wrap;html=1;fillColor=#F5F5F5;strokeColor=#CCCCCC;"
    "fontColor=#666666;fontSize=12;fontStyle=2;verticalAlign=top;align=left;"
    "spacingLeft=8;spacingTop=6;dashed=1;dashPattern=8 4;"
)


def esc(text: str) -> str:
    return html.escape(text, quote=True)


def center(x: int, y: int, w: int, h: int) -> tuple[float, float]:
    return x + w / 2, y + h / 2


def midpoint(a: tuple[float, float], b: tuple[float, float]) -> tuple[float, float]:
    return (a[0] + b[0]) / 2, (a[1] + b[1]) / 2


def entity_box(eid: str, x: int, y: int) -> tuple[int, int, int, int]:
    return x, y, EW, EH


def place_diamond(
    src: str,
    dst: str,
    nodes: dict[str, tuple[int, int, int, int]],
    ratio: float = 0.45,
) -> tuple[int, int]:
    sx, sy = center(*nodes[src])
    dx, dy = center(*nodes[dst])
    cx = sx + ratio * (dx - sx)
    cy = sy + ratio * (dy - sy)
    return int(cx - RW / 2), int(cy - RH / 2)


def rect_cell(cid: str, label: str, x: int, y: int, w: int, h: int, diamond: bool = False) -> str:
    style = DIAMOND_STYLE if diamond else RECT_STYLE
    return (
        f'        <mxCell id="{cid}" value="{esc(label)}" style="{style}" '
        f'vertex="1" parent="1">\n'
        f'          <mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry" />\n'
        f"        </mxCell>"
    )


def zone_cell(zid: str, label: str, x: int, y: int, w: int, h: int) -> str:
    return (
        f'        <mxCell id="{zid}" value="{esc(label)}" style="{ZONE_STYLE}" '
        f'vertex="1" parent="1">\n'
        f'          <mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry" />\n'
        f"        </mxCell>"
    )


def edge_cell(
    eid: str,
    source: str,
    target: str,
    waypoints: list[tuple[int, int]] | None = None,
) -> str:
    if not waypoints:
        return (
            f'        <mxCell id="{eid}" style="{EDGE_STYLE}" edge="1" parent="1" '
            f'source="{source}" target="{target}">\n'
            f'          <mxGeometry relative="1" as="geometry" />\n'
            f"        </mxCell>"
        )
    pts = "\n".join(f'            <mxPoint x="{x}" y="{y}" />' for x, y in waypoints)
    return (
        f'        <mxCell id="{eid}" style="{EDGE_STYLE}" edge="1" parent="1" '
        f'source="{source}" target="{target}">\n'
        f'          <mxGeometry relative="1" as="geometry">\n'
        f'            <Array as="points">\n{pts}\n            </Array>\n'
        f"          </mxGeometry>\n"
        f"        </mxCell>"
    )


def text_cell(tid: str, label: str, x: float, y: float) -> str:
    return (
        f'        <mxCell id="{tid}" value="{esc(label)}" style="{CARD_STYLE}" '
        f'vertex="1" connectable="0" parent="1">\n'
        f'          <mxGeometry x="{x:.0f}" y="{y:.0f}" width="24" height="20" as="geometry" />\n'
        f"        </mxCell>"
    )


def build_drawio() -> str:
    nodes: dict[str, tuple[int, int, int, int]] = {}
    for eid, _, x, y in ENTITIES:
        nodes[eid] = entity_box(eid, x, y)

    rel_positions: dict[str, tuple[int, int, int, int]] = {}
    for src, rel, dst, _, _ in LINKS:
        if rel in RELATION_OVERRIDES:
            ox, oy = RELATION_OVERRIDES[rel]
            rx, ry = ox - RW // 2, oy - RH // 2
        else:
            rx, ry = place_diamond(src, dst, nodes)
        rel_positions[rel] = (rx, ry, RW, RH)
        nodes[rel] = rel_positions[rel]

    cells: list[str] = [
        '        <mxCell id="0" />',
        '        <mxCell id="1" parent="0" />',
        (
            '        <mxCell id="title" value="医院药品管理系统 E-R 图（Chen 记法 · 精简）" '
            f'style="{TITLE_STYLE}" vertex="1" parent="1">\n'
            f'          <mxGeometry x="220" y="16" width="660" height="36" as="geometry" />\n'
            "        </mxCell>"
        ),
        (
            '        <mxCell id="legend" value="图例：&#xa;■ 矩形 = 实体&#xa;◆ 菱形 = 联系&#xa;1 / N = 基数" '
            f'style="{LEGEND_STYLE}" vertex="1" parent="1">\n'
            '          <mxGeometry x="40" y="56" width="160" height="64" as="geometry" />\n'
            "        </mxCell>"
        ),
        (
            '        <mxCell id="note" value="已省略：库存预警、审核记录、盘点、锁定、菜单、公告等辅助实体" '
            'style="text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=top;fontSize=11;fontColor=#888888;" '
            'vertex="1" parent="1">\n'
            '          <mxGeometry x="40" y="600" width="420" height="24" as="geometry" />\n'
            "        </mxCell>"
        ),
    ]

    for i, (title, x, y, w, h) in enumerate(ZONES):
        cells.append(zone_cell(f"zone_{i}", title, x, y, w, h))

    for eid, label, x, y in ENTITIES:
        cells.append(rect_cell(eid, label, x, y, EW, EH, diamond=False))

    rel_labels = dict(RELATIONS)
    for rid, (rx, ry, rw, rh) in rel_positions.items():
        cells.append(rect_cell(rid, rel_labels[rid], rx, ry, rw, rh, diamond=True))

    edge_idx = 0
    card_idx = 0
    for src, rel, dst, c1, c2 in LINKS:
        wp_pair = EDGE_WAYPOINTS.get((src, rel, dst))
        wp1, wp2 = wp_pair if wp_pair else (None, None)
        cells.append(edge_cell(f"edge_{edge_idx}", src, rel, wp1))
        edge_idx += 1
        cells.append(edge_cell(f"edge_{edge_idx}", rel, dst, wp2))
        edge_idx += 1

        sx, sy = center(*nodes[src])
        rx, ry = center(*nodes[rel])
        dx, dy = center(*nodes[dst])
        m1 = midpoint((sx, sy), (rx, ry))
        m2 = midpoint((rx, ry), (dx, dy))
        cells.append(text_cell(f"card_{card_idx}", c1, m1[0] - 10, m1[1] - 10))
        card_idx += 1
        cells.append(text_cell(f"card_{card_idx}", c2, m2[0] - 10, m2[1] - 10))
        card_idx += 1

    body = "\n".join(cells)
    return f"""<mxfile host="app.diagrams.net" modified="2026-06-03T00:00:00.000Z" agent="generate_chen_er_diagram.py" version="22.1.0" type="device">
  <diagram name="系统ER图" id="chen-er-drug-management">
    <mxGraphModel dx="1422" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="{PAGE_W}" pageHeight="{PAGE_H}" math="0" shadow="0">
      <root>
{body}
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>
"""


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(build_drawio(), encoding="utf-8")
    print("生成成功!")
    print(f"  输出文件: {OUTPUT}")
    print("  实体: 10 个 | 联系: 11 个（精简版）")


if __name__ == "__main__":
    main()
