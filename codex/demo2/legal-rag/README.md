# Legal RAG - 面向法律文档的层级化检索增强生成系统

## 项目概述

本系统实现了一套面向法律文档的层级化 RAG（Retrieval-Augmented Generation）问答系统，核心创新包括：

1. **层级感知分块算法** — 自动识别法律文档的"章-节-条-款-项"层级结构，以"条"为基本检索单元，保留上下文层级信息
2. **多粒度混合检索与层级重排序** — 稠密检索（BGE）+ 稀疏检索（BM25）混合召回，结合层级距离特征的重排序
3. **端到端引用溯源与幻觉抑制** — 生成阶段自动标注法条来源，配合 NLI 校验降低幻觉

## 项目结构

```
legal-rag/
├── src/
│   ├── chunking/
│   │   └── legal_chunker.py      # 层级感知分块算法
│   ├── retrieval/
│   │   └── hybrid_retriever.py    # 混合检索与层级重排序
│   ├── generation/
│   │   └── generator.py           # LLM 生成与引用标注
│   ├── hallucination/
│   │   └── detector.py            # 幻觉检测与抑制
│   ├── api/
│   │   └── server.py              # FastAPI REST 服务
│   └── pipeline.py                # RAG 流水线编排
├── data/
│   ├── sample_legal_docs/         # 示例法律文档
│   └── processed/                 # 处理后的分块数据
├── webui/
│   └── index.html                 # Web 交互界面
├── tests/
│   └── test_pipeline.py           # 测试脚本
├── requirements.txt
└── README.md
```

## 安装与运行

### 环境要求

- Python 3.9+
- 推荐：8GB+ RAM（CPU 模式），或 NVIDIA GPU（CUDA 模式）

### 安装依赖

```bash
cd legal-rag
pip install -r requirements.txt
```

### 运行测试

```bash
python tests/test_pipeline.py
```

### 启动 API 服务

```bash
python src/api/server.py
```

服务启动后访问 http://127.0.0.1:8000 查看 API 状态。

### 启动 Web 界面

直接浏览器打开 `webui/index.html` 即可使用（需同时运行 API 服务）。

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 系统状态 |
| POST | /query | 法律问答 |
| POST | /index | 索引单个文档 |
| POST | /index-directory | 索引目录下所有文档 |

### 示例请求

```bash
curl -X POST http://127.0.0.1:8000/query \
  -H "Content-Type: application/json" \
  -d '{"question": "合同应当包括哪些主要条款？", "top_k": 5}'
```

## 技术栈

- **嵌入模型**: BAAI/bge-small-zh-v1.5
- **稀疏检索**: BM25 (rank_bm25)
- **生成模型**: Qwen2.5-7B-Instruct (支持 LoRA 微调)
- **API 框架**: FastAPI + Uvicorn
- **前端**: 原生 HTML/CSS/JS
