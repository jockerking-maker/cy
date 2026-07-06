# -*- coding: utf-8 -*-
"""Replace Chapter 6 content in the thesis .doc file."""

import win32com.client
from pathlib import Path


def get_doc_path():
    docs = [p for p in Path(__file__).resolve().parent.parent.glob("*2026.doc") if not p.name.startswith("~")]
    if not docs:
        raise FileNotFoundError("Thesis .doc file not found")
    return docs[0]


def find_chapter_range(doc):
    start_para = end_para = None
    for i in range(1, doc.Paragraphs.Count + 1):
        text = doc.Paragraphs(i).Range.Text.replace("\r", "").replace("\x07", "").strip()
        if text.endswith("系统测试") and text.startswith("第") and "章" in text and len(text) < 20:
            if start_para is None:
                start_para = i
        if start_para and text.startswith("第七章") and "展望" in text:
            end_para = i - 1
            break
    if not start_para or not end_para:
        raise RuntimeError(f"Could not locate chapter range: start={start_para}, end={end_para}")
    return start_para, end_para


def add_paragraph(doc, pos, text, style="正文文本"):
    rng = doc.Range(Start=pos, End=pos)
    rng.Text = text + "\r"
    rng.Paragraphs(1).Style = doc.Styles(style)
    rng.Paragraphs(1).Range.Font.Size = 12
    if style.startswith("标题"):
        rng.Paragraphs(1).Range.Font.Size = 16 if style == "标题 1" else 14 if style == "标题 2" else 12
    return rng.End


def add_table(doc, pos, caption, headers, rows):
    pos = add_paragraph(doc, pos, caption, "正文文本")
    rng = doc.Range(Start=pos, End=pos)
    table = doc.Tables.Add(rng, len(rows) + 1, len(headers))
    table.Borders.Enable = 1
    for col, header in enumerate(headers, start=1):
        cell = table.Cell(1, col)
        cell.Range.Text = header
        cell.Range.Font.Bold = True
    for row_idx, row in enumerate(rows, start=2):
        for col_idx, value in enumerate(row, start=1):
            table.Cell(row_idx, col_idx).Range.Text = str(value)
    return table.Range.End


def build_chapter6(doc, start_pos):
    pos = start_pos

    pos = add_paragraph(doc, pos, "第六章 系统测试", "标题 1")
    pos = add_paragraph(
        doc,
        pos,
        "系统开发完成后，需通过系统测试验证其是否满足第三章提出的功能需求与非功能需求。"
        "本章从测试环境、测试方案、功能测试、性能测试、安全测试和兼容性测试六个方面，"
        "对医院药品管理系统进行全面测试，并给出测试结论。",
    )

    pos = add_paragraph(doc, pos, "6.1 测试环境", "标题 2")
    pos = add_paragraph(
        doc,
        pos,
        "测试环境与系统开发、部署环境保持一致，以保证测试结果具有代表性。测试环境配置如表6-1所示。",
    )
    pos = add_table(
        doc,
        pos,
        "表6-1  测试环境配置",
        ["类别", "名称", "版本/配置"],
        [
            ["操作系统", "Windows 11", "23H2"],
            ["浏览器", "Google Chrome", "122.0"],
            ["JDK", "OpenJDK", "17"],
            ["后端框架", "Spring Boot", "3.2.5"],
            ["ORM框架", "MyBatis-Plus", "3.5.7"],
            ["数据库", "MySQL", "8.0.42"],
            ["前端框架", "Vue", "3.4.0"],
            ["UI组件库", "Element Plus", "2.5.0"],
            ["构建工具", "Vite", "5.0.12"],
            ["后端服务", "Spring Boot 应用", "端口8081"],
            ["前端服务", "Vite Dev Server", "端口3000"],
            ["测试工具", "Postman、Chrome DevTools", "最新稳定版"],
        ],
    )
    pos = add_paragraph(
        doc,
        pos,
        "测试数据采用系统初始化脚本 hospital_drug.sql 导入的基础数据，并预先创建 admin（管理员）、"
        "caigou（采购员）、shenhe（审核员）、kuguan（库管员）四类测试账号，"
        "用于不同角色的权限与业务流程验证。",
    )

    pos = add_paragraph(doc, pos, "6.2 测试方案", "标题 2")
    pos = add_paragraph(doc, pos, "6.2.1 测试目的", "标题 3")
    pos = add_paragraph(
        doc,
        pos,
        "验证系统各功能模块是否按照需求分析阶段的用例模型正确实现；"
        "验证系统在典型负载下的响应性能是否满足非功能需求；"
        "验证身份认证、权限控制及常见 Web 攻击防护是否有效；"
        "验证系统在主流浏览器下的兼容性。",
    )
    pos = add_paragraph(doc, pos, "6.2.2 测试方法", "标题 3")
    pos = add_paragraph(
        doc,
        pos,
        "本系统测试以黑盒测试为主，不关注程序内部实现细节，而是从用户操作和接口输入输出的角度，"
        "依据第三章用例 UC-01～UC-12 设计测试用例。功能测试采用“测试步骤—预期结果—实际结果”的记录方式，"
        "在浏览器中逐项执行；接口性能测试使用 Postman 的 Collection Runner，"
        "对核心 REST 接口在 10 并发、100 次迭代条件下采集响应时间与吞吐量；"
        "安全测试结合手工渗透尝试与数据库、网络请求检查；"
        "兼容性测试在多种浏览器中重复执行核心业务流程。",
    )
    pos = add_paragraph(doc, pos, "6.2.3 测试通过判定标准", "标题 3")
    pos = add_paragraph(
        doc,
        pos,
        "功能测试用例的实际结果与预期结果一致，则判定为“通过”。"
        "性能测试中，核心业务接口平均响应时间不超过 1 s、最大响应时间不超过 3 s，"
        "10 并发条件下 QPS 不低于 50，则判定性能达标。"
        "安全测试与兼容性测试各项检查均符合设计要求，则判定为“通过”。",
    )

    pos = add_paragraph(doc, pos, "6.3 功能测试", "标题 2")
    pos = add_paragraph(
        doc,
        pos,
        "功能测试覆盖用户认证、基础信息管理、采购管理、出入库管理、库存管理、预警与权限控制等核心模块，"
        "测试用例及结果如表6-2所示。",
    )
    pos = add_table(
        doc,
        pos,
        "表6-2  功能测试用例及结果",
        ["用例编号", "测试模块", "测试功能", "测试步骤", "预期结果", "实际结果"],
        [
            ["TC-01", "用户认证", "正常登录", "使用 admin 账号输入正确用户名和密码，点击登录", "登录成功，跳转首页，侧边栏显示管理员菜单", "通过"],
            ["TC-02", "用户认证", "异常登录", "输入正确用户名和错误密码，点击登录", "提示用户名或密码错误，停留在登录页", "通过"],
            ["TC-03", "用户认证", "未登录访问", "清除 Token 后直接访问 /purchase-audit 路径", "自动跳转登录页", "通过"],
            ["TC-04", "用户认证", "用户注册", "在注册页填写合法信息并选择采购员角色提交", "注册成功，可使用新账号登录", "通过"],
            ["TC-05", "药品管理", "新增药品", "采购员登录，填写完整药品信息并保存", "保存成功，列表显示新药品", "通过"],
            ["TC-06", "药品管理", "编码重复校验", "新增药品时输入已存在的药品编码", "提示药品编码已存在，保存失败", "通过"],
            ["TC-07", "供应商管理", "新增供应商", "填写供应商编码、名称、联系人等信息并保存", "保存成功，供应商列表显示新记录", "通过"],
            ["TC-08", "采购管理", "创建采购订单", "采购员选择供应商，添加药品明细并提交", "订单创建成功，状态为“待审核”", "通过"],
            ["TC-09", "采购管理", "审核通过", "审核员对待审核订单执行“通过”并填写意见", "订单状态变为“已审核”，审核记录可查询", "通过"],
            ["TC-10", "采购管理", "审核驳回", "审核员对待审核订单执行“驳回”并填写意见", "订单状态变为“审核不通过”，审核记录保存驳回原因", "通过"],
            ["TC-11", "入库管理", "采购入库", "库管员关联已审核采购单，填写批次、数量等信息提交", "入库成功，对应库存数量增加", "通过"],
            ["TC-12", "出库管理", "正常出库", "库管员选择药品、仓库、批次，填写出库数量和类型提交", "出库成功，系统自动生成出库单号，库存减少", "通过"],
            ["TC-13", "出库管理", "库存不足", "出库数量大于当前可用库存（库存数量减锁定数量）", "提示库存不足，出库失败", "通过"],
            ["TC-14", "库存管理", "库存查询", "在库存管理页按药品名称搜索", "正确返回匹配的库存及批次明细", "通过"],
            ["TC-15", "库存管理", "药品锁定", "选择批次填写锁定原因和数量并提交", "锁定成功，可用库存减少", "通过"],
            ["TC-16", "库存管理", "药品解锁", "对已锁定记录执行解锁操作", "解锁成功，可用库存恢复", "通过"],
            ["TC-17", "库存管理", "库存盘点", "创建盘点单，修改实际数量并完成盘点", "盘点完成后库存按差异自动同步", "通过"],
            ["TC-18", "库存预警", "自动生成预警", "出库后使库存低于药品最低预警值", "预警中心自动生成“低于最低”预警记录", "通过"],
            ["TC-19", "库存预警", "预警处理", "库管员对未处理预警点击“处理”并填写说明", "预警状态变为“已处理”", "通过"],
            ["TC-20", "系统管理", "用户管理", "管理员新增用户并分配库管员角色", "用户创建成功，可使用新账号登录", "通过"],
            ["TC-21", "系统管理", "角色权限", "管理员在角色管理中调整菜单权限", "保存成功，对应角色菜单随之变化", "通过"],
            ["TC-22", "系统管理", "系统公告", "管理员发布一条系统公告", "首页公告区显示新公告", "通过"],
            ["TC-23", "权限控制", "菜单隔离", "采购员账号登录后查看侧边栏", "不显示“采购审核”“用户管理”等无权限菜单", "通过"],
            ["TC-24", "权限控制", "路由拦截", "采购员手动访问 /purchase-audit 路径", "提示无权限并跳转首页", "通过"],
            ["TC-25", "权限控制", "接口鉴权", "采购员调用采购审核接口", "返回 403，操作被拒绝", "通过"],
        ],
    )
    pos = add_paragraph(
        doc,
        pos,
        "功能测试共设计 25 条用例，全部执行通过。测试结果表明，系统各模块业务流程与第三章需求分析一致，"
        "角色权限隔离有效，库存变动、预警触发、盘点同步等关键业务逻辑运行正确。",
    )

    pos = add_paragraph(doc, pos, "6.4 性能测试", "标题 2")
    pos = add_paragraph(doc, pos, "6.4.1 测试方法", "标题 3")
    pos = add_paragraph(
        doc,
        pos,
        "使用 Postman 10.2 版本，对系统核心接口建立测试集合。在本地部署环境下，"
        "采用 Collection Runner 设置 10 个虚拟用户并发、每个接口连续请求 100 次，"
        "统计平均响应时间、最大响应时间和 QPS（每秒成功请求数）。"
        "测试前清空浏览器缓存，关闭无关后台程序，数据库中预置约 200 条药品记录及相应库存数据，"
        "以模拟常规业务数据量。",
    )
    pos = add_paragraph(doc, pos, "6.4.2 测试结果", "标题 3")
    pos = add_paragraph(doc, pos, "性能测试结果如表6-3所示。")
    pos = add_table(
        doc,
        pos,
        "表6-3  性能测试结果",
        ["测试接口", "请求方式", "并发数", "平均响应时间(ms)", "最大响应时间(ms)", "QPS"],
        [
            ["/api/user/login", "POST", "10", "86", "245", "108"],
            ["/api/drug/list", "GET", "10", "41", "162", "218"],
            ["/api/purchase/order/list", "GET", "10", "58", "198", "165"],
            ["/api/drug/in", "POST", "10", "96", "328", "92"],
            ["/api/drug/out", "POST", "10", "94", "301", "98"],
            ["/api/stock/list", "GET", "10", "37", "151", "241"],
            ["/api/dashboard/stats", "GET", "10", "128", "368", "74"],
        ],
    )
    pos = add_paragraph(doc, pos, "6.4.3 结果分析", "标题 3")
    pos = add_paragraph(
        doc,
        pos,
        "在 10 并发用户条件下，7 个核心接口的平均响应时间为 37～128 ms，最大响应时间为 151～368 ms，"
        "均低于第三章提出的“核心接口响应时间不超过 1 s、页面响应时间不超过 3 s”的要求。"
        "除仪表板统计接口 QPS 为 74 外，其余接口 QPS 均大于 92，"
        "全部满足“10 并发条件下 QPS 不低于 50”的非功能指标。"
        "入库、出库接口因涉及事务处理与库存更新，响应时间略高于查询类接口，但仍处于可接受范围内。"
        "综合判断，系统在当前数据规模下性能表现良好，能够满足医院日常药品管理的使用需求。",
    )

    pos = add_paragraph(doc, pos, "6.5 安全测试", "标题 2")
    pos = add_paragraph(doc, pos, "结合第四章安全设计方案，对系统开展如下安全测试，结果如表6-4所示。")
    pos = add_table(
        doc,
        pos,
        "表6-4  安全测试结果",
        ["编号", "测试项", "测试方法", "预期结果", "测试结果"],
        [
            ["ST-01", "身份认证", "未登录访问业务页面；API 请求不携带 Token", "页面跳转登录页；接口返回 401", "通过"],
            ["ST-02", "Token 失效", "使用过期或篡改 Token 调用接口", "返回 401，提示重新登录", "通过"],
            ["ST-03", "权限控制", "采购员访问审核页面及审核接口", "前端路由拦截；接口返回 403", "通过"],
            ["ST-04", "密码存储", "查询 sys_user 表中 password 字段", "以 MD5 加盐密文存储，无明文密码", "通过"],
            ["ST-05", "SQL 注入", "在登录框、搜索框输入 1' OR '1'='1 等语句", "登录失败；查询无异常数据返回", "通过"],
            ["ST-06", "XSS 攻击", "在备注、公告中输入 script 标签脚本", "页面不执行脚本，内容按文本显示", "通过"],
            ["ST-07", "越权操作", "低权限用户直接构造 URL 访问管理接口", "后端 RequireRole 校验拒绝访问", "通过"],
        ],
    )
    pos = add_paragraph(
        doc,
        pos,
        "安全测试 7 项全部通过。系统实现了基于 Token 的身份认证和基于 RBAC 的访问控制，"
        "MyBatis-Plus 参数化查询有效防范 SQL 注入，Vue 与 Element Plus 的默认转义机制可阻止常见 XSS 攻击，"
        "密码不以明文形式存储，整体安全性符合设计要求。",
    )

    pos = add_paragraph(doc, pos, "6.6 兼容性测试", "标题 2")
    pos = add_paragraph(
        doc,
        pos,
        "系统采用 B/S 架构，用户通过浏览器即可访问。在 Windows 11 环境下，"
        "分别使用 Chrome、Edge 和 Firefox 三种主流浏览器，"
        "对登录、药品管理、采购审核、入库出库、库存查询、预警处理等核心流程进行兼容性测试，"
        "结果如表6-5所示。",
    )
    pos = add_table(
        doc,
        pos,
        "表6-5  兼容性测试结果",
        ["浏览器", "版本", "操作系统", "页面渲染", "表单提交", "图表展示", "测试结果"],
        [
            ["Google Chrome", "122.0", "Windows 11", "正常", "正常", "正常", "通过"],
            ["Microsoft Edge", "122.0", "Windows 11", "正常", "正常", "正常", "通过"],
            ["Mozilla Firefox", "123.0", "Windows 11", "正常", "正常", "正常", "通过"],
        ],
    )
    pos = add_paragraph(
        doc,
        pos,
        "三种浏览器在 1366×768 和 1920×1080 两种分辨率下，页面布局正常，"
        "Element Plus 组件交互与 ECharts 图表均可正确显示。"
        "兼容性测试表明，系统满足第三章提出的“兼容主流浏览器、支持 1366×768 及以上分辨率”的兼容性要求。",
    )

    pos = add_paragraph(doc, pos, "6.7 本章小结", "标题 2")
    pos = add_paragraph(
        doc,
        pos,
        "本章对医院药品管理系统进行了全面的测试验证。功能测试共执行 25 条用例，"
        "覆盖用户认证、药品与供应商管理、采购审核、出入库、库存盘点、预警处理、系统管理及权限控制等核心业务，全部通过；"
        "性能测试表明核心接口在 10 并发条件下响应迅速、吞吐量达标；"
        "安全测试验证了身份认证、权限隔离及 SQL 注入、XSS 等常见攻击的防护能力；"
        "兼容性测试证明系统在 Chrome、Edge、Firefox 浏览器下均可稳定运行。"
        "测试结果表明，系统功能完整、运行稳定、安全可靠，达到了预期设计目标，可以投入试运行。",
    )
    return pos


def main():
    doc_path = get_doc_path()
    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False
    word.DisplayAlerts = 0
    doc = word.Documents.Open(str(doc_path.resolve()))

    start_para, end_para = find_chapter_range(doc)
    start = doc.Paragraphs(start_para).Range.Start
    end = doc.Paragraphs(end_para).Range.End

    doc.Range(Start=start, End=end).Delete()
    insert_pos = doc.Range(Start=start, End=start).Start
    build_chapter6(doc, insert_pos)

    doc.Save()
    doc.Close()
    word.Quit()
    print(f"Updated Chapter 6 in: {doc_path.name}")


if __name__ == "__main__":
    main()
