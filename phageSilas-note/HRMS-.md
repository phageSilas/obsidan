<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<title>HRMS 考勤管理与薪资管理模块系分</title>
<style>
  @page {
    size: A4;
    margin: 22mm 18mm;
  }
  :root {
    --bg: #f0ede5;
    --paper: #ffffff;
    --ink: #1a1d21;
    --ink-2: #4a5159;
    --ink-3: #8a8f96;
    --line: #e2ddd1;
    --line-soft: #efebe0;
    --accent: #1e4d8c;
    --accent-2: #c8553d;
    --code-bg: #f6f3ec;
    --code-border: #d8d2c2;
    --tag-bg: #eef1f6;
  }
  * { box-sizing: border-box; }
  html, body {
    margin: 0;
    padding: 0;
  }
  body {
    background: var(--bg);
    font-family: 'Source Han Sans SC', 'PingFang SC', 'Microsoft YaHei', 'Noto Sans CJK SC', sans-serif;
    color: var(--ink);
    font-size: 14px;
    line-height: 1.75;
    -webkit-font-smoothing: antialiased;
  }
  .doc {
    max-width: 900px;
    margin: 0 auto;
    background: var(--paper);
    padding: 70px 80px 90px;
    box-shadow: 0 1px 6px rgba(0,0,0,0.08);
  }
  /* ---------- Cover ---------- */
  .cover {
    min-height: 1080px;
    padding: 100px 0 60px;
    page-break-after: always;
    position: relative;
    border-bottom: 1px solid var(--line);
  }
  .cover .crumb {
    font-size: 11px;
    letter-spacing: 3px;
    color: var(--ink-3);
    text-transform: uppercase;
    margin-bottom: 28px;
  }
  .cover h1 {
    font-size: 52px;
    line-height: 1.15;
    font-weight: 800;
    margin: 0 0 24px;
    letter-spacing: -1px;
  }
  .cover h1 .em {
    color: var(--accent);
  }
  .cover .sub {
    font-size: 18px;
    color: var(--ink-2);
    font-weight: 400;
    margin-bottom: 70px;
  }
  .cover .rule {
    width: 60px;
    height: 4px;
    background: var(--accent);
    margin-bottom: 40px;
  }
  .cover .meta-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 28px 60px;
    margin-bottom: 80px;
    border-top: 1px solid var(--line);
    padding-top: 32px;
  }
  .cover .meta-grid .item {
    display: flex;
    flex-direction: column;
  }
  .cover .meta-grid .lbl {
    font-size: 11px;
    color: var(--ink-3);
    letter-spacing: 1.5px;
    margin-bottom: 6px;
    text-transform: uppercase;
  }
  .cover .meta-grid .val {
    font-size: 15px;
    color: var(--ink);
    font-weight: 500;
  }
  .cover .modules {
    margin-top: 40px;
    border-top: 1px solid var(--line);
    padding-top: 28px;
  }
  .cover .modules h3 {
    font-size: 11px;
    letter-spacing: 2px;
    color: var(--ink-3);
    margin: 0 0 16px;
    text-transform: uppercase;
  }
  .cover .module-list {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 18px;
  }
  .cover .module-card {
    border: 1px solid var(--line);
    padding: 22px 24px;
    background: #fbfaf6;
  }
  .cover .module-card .num {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 12px;
    color: var(--accent);
    margin-bottom: 8px;
    font-weight: 600;
  }
  .cover .module-card .ttl {
    font-size: 17px;
    font-weight: 700;
    margin-bottom: 6px;
  }
  .cover .module-card .desc {
    font-size: 12px;
    color: var(--ink-2);
    line-height: 1.6;
  }
  .cover .footer {
    position: absolute;
    bottom: 60px;
    left: 0;
    right: 0;
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: var(--ink-3);
    letter-spacing: 1px;
  }
  /* ---------- TOC ---------- */
  .toc {
    page-break-after: always;
    padding: 30px 0;
  }
  .toc h2 {
    font-size: 13px;
    letter-spacing: 3px;
    color: var(--ink-3);
    text-transform: uppercase;
    margin: 0 0 28px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--line);
  }
  .toc .group {
    margin-bottom: 32px;
  }
  .toc .group-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--accent);
    margin-bottom: 12px;
  }
  .toc .group-title .gn {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 13px;
    color: var(--ink-3);
    margin-right: 10px;
  }
  .toc ul {
    list-style: none;
    padding: 0;
    margin: 0;
  }
  .toc li {
    display: flex;
    align-items: baseline;
    padding: 4px 0;
    font-size: 13px;
    color: var(--ink-2);
  }
  .toc li .ln {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 11px;
    color: var(--ink-3);
    margin-right: 14px;
    min-width: 32px;
  }
  .toc li .dt {
    flex: 1;
    border-bottom: 1px dotted var(--line);
    margin: 0 8px;
    position: relative;
    top: -3px;
  }
  /* ---------- Section ---------- */
  .module-section {
    page-break-before: always;
    padding-top: 20px;
  }
  .module-banner {
    border-top: 4px solid var(--accent);
    padding-top: 18px;
    margin-bottom: 40px;
    display: flex;
    align-items: baseline;
    justify-content: space-between;
  }
  .module-banner .left {
    display: flex;
    align-items: baseline;
    gap: 18px;
  }
  .module-banner .code {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 14px;
    color: var(--accent);
    font-weight: 600;
    letter-spacing: 1px;
  }
  .module-banner .title {
    font-size: 30px;
    font-weight: 800;
    letter-spacing: -0.5px;
  }
  .module-banner .right {
    font-size: 11px;
    color: var(--ink-3);
    letter-spacing: 2px;
  }
  h2.sec {
    font-size: 19px;
    font-weight: 700;
    color: var(--ink);
    margin: 44px 0 18px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--line);
    display: flex;
    align-items: baseline;
    gap: 12px;
  }
  h2.sec .sn {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 13px;
    color: var(--accent);
    font-weight: 600;
  }
  h3.sub {
    font-size: 14px;
    font-weight: 600;
    color: var(--ink);
    margin: 26px 0 12px;
    padding-left: 12px;
    border-left: 3px solid var(--accent);
  }
  p {
    margin: 0 0 12px;
    color: var(--ink);
  }
  ul, ol {
    margin: 8px 0 16px;
    padding-left: 22px;
  }
  ul.bullets {
    list-style: none;
    padding-left: 0;
  }
  ul.bullets li {
    position: relative;
    padding-left: 18px;
    margin-bottom: 8px;
    color: var(--ink);
  }
  ul.bullets li::before {
    content: '';
    position: absolute;
    left: 4px;
    top: 11px;
    width: 5px;
    height: 5px;
    background: var(--accent);
  }
  /* ---------- Tables ---------- */
  .table-wrap {
    margin: 14px 0 20px;
    overflow-x: auto;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12.5px;
  }
  table th, table td {
    border: 1px solid var(--line);
    padding: 9px 12px;
    text-align: left;
    vertical-align: top;
  }
  table th {
    background: #f7f4ec;
    font-weight: 600;
    color: var(--ink);
    font-size: 12px;
  }
  table td {
    color: var(--ink-2);
  }
  table td.req, table td.yes {
    color: var(--accent);
    font-weight: 600;
  }
  table tr:nth-child(even) td {
    background: #fbfaf6;
  }
  /* ---------- Code ---------- */
  pre {
    background: var(--code-bg);
    border: 1px solid var(--code-border);
    border-left: 3px solid var(--accent);
    padding: 16px 20px;
    margin: 14px 0 22px;
    overflow-x: auto;
    font-family: 'JetBrains Mono', 'SF Mono', Consolas, 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.65;
    color: var(--ink);
    white-space: pre;
  }
  pre .cmt { color: #8a8f96; }
  pre .kw { color: var(--accent); font-weight: 600; }
  code.inline {
    font-family: 'JetBrains Mono', Consolas, monospace;
    background: var(--code-bg);
    padding: 2px 6px;
    border-radius: 2px;
    font-size: 12px;
    color: var(--accent-2);
  }
  /* ---------- API ---------- */
  .api {
    margin: 22px 0;
    border: 1px solid var(--line);
    background: #fbfaf6;
  }
  .api .head {
    padding: 12px 18px;
    border-bottom: 1px solid var(--line);
    display: flex;
    align-items: center;
    gap: 12px;
    background: #f3efe3;
  }
  .api .method {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 8px;
    background: var(--accent);
    color: #fff;
    letter-spacing: 1px;
  }
  .api .path {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 12.5px;
    color: var(--ink);
    font-weight: 600;
  }
  .api .desc {
    font-size: 12px;
    color: var(--ink-3);
    margin-left: auto;
  }
  .api .body {
    padding: 14px 18px;
  }
  .api .body h4 {
    font-size: 11px;
    color: var(--ink-3);
    letter-spacing: 1.5px;
    margin: 8px 0 8px;
    text-transform: uppercase;
  }
  /* ---------- Footer ---------- */
  .footer-pg {
    margin-top: 60px;
    padding-top: 18px;
    border-top: 1px solid var(--line);
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: var(--ink-3);
    letter-spacing: 1px;
  }
  /* ---------- Schedule ---------- */
  .schedule {
    margin: 16px 0;
  }
  .schedule-row {
    display: grid;
    grid-template-columns: 40px 1fr 80px;
    gap: 16px;
    padding: 12px 0;
    border-bottom: 1px solid var(--line-soft);
    align-items: center;
  }
  .schedule-row .idx {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 12px;
    color: var(--accent);
    font-weight: 600;
  }
  .schedule-row .stage {
    font-weight: 600;
    font-size: 13.5px;
  }
  .schedule-row .stage small {
    display: block;
    font-weight: 400;
    color: var(--ink-2);
    font-size: 12px;
    margin-top: 2px;
  }
  .schedule-row .dur {
    text-align: right;
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 13px;
    color: var(--ink);
    font-weight: 600;
  }
  .schedule-total {
    margin-top: 20px;
    padding: 14px 18px;
    background: var(--accent);
    color: #fff;
    font-size: 13px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .schedule-total .v {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 16px;
    font-weight: 700;
  }
  /* ---------- Misc ---------- */
  .note {
    background: #eef1f6;
    border-left: 3px solid var(--accent);
    padding: 12px 16px;
    margin: 14px 0;
    font-size: 12.5px;
    color: var(--ink-2);
  }
  .note strong { color: var(--accent); }
  .participants {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 10px;
    margin: 14px 0;
  }
  .participants .p {
    border: 1px solid var(--line);
    padding: 12px;
    text-align: center;
    background: #fbfaf6;
  }
  .participants .p .r {
    font-size: 11px;
    color: var(--ink-3);
    margin-bottom: 6px;
    letter-spacing: 1px;
  }
  .participants .p .n {
    font-size: 13px;
    font-weight: 600;
  }
  @media print {
    body { background: #fff; }
    .doc { box-shadow: none; max-width: none; padding: 0; }
  }
</style>
</head>
<body>
<div class="doc">
<!-- ====================== COVER ====================== -->
<section class="cover">
  <div class="crumb">HRMS · System Analysis & Design</div>
  <h1>考勤管理 <span class="em">·</span> 薪资管理<br>模块系分文档</h1>
  <div class="sub">Human Resource Management System — Module Specification</div>
  <div class="rule"></div>
  <div class="meta-grid">
    <div class="item"><span class="lbl">Document</span><span class="val">HRMS-ATT-SAL-SAD</span></div>
    <div class="item"><span class="lbl">Version</span><span class="val">1.0 (初稿)</span></div>
    <div class="item"><span class="lbl">Date</span><span class="val">2026-07-09</span></div>
    <div class="item"><span class="lbl">Author</span><span class="val">凯贾</span></div>
    <div class="item"><span class="lbl">Source</span><span class="val">HRMS 产品规格说明书 §6 / §7</span></div>
    <div class="item"><span class="lbl">Status</span><span class="val">Draft for Review</span></div>
  </div>
  <div class="modules">
    <h3>Modules in Scope</h3>
    <div class="module-list">
      <div class="module-card">
        <div class="num">M-01 / §6</div>
        <div class="ttl">考勤管理</div>
        <div class="desc">考勤规则配置、网页打卡、请假流程、月度统计与可视化。</div>
      </div>
      <div class="module-card">
        <div class="num">M-02 / §7</div>
        <div class="ttl">薪资管理</div>
        <div class="desc">薪资账套、员工薪资档案、月度核算、异常检测、工资条。</div>
      </div>
    </div>
  </div>
  <div class="footer">
    <span>© HRMS Engineering</span>
    <span>Confidential — Internal Use</span>
  </div>
</section>
<!-- ====================== TOC ====================== -->
<section class="toc">
  <h2>Table of Contents</h2>
  <div class="group">
    <div class="group-title"><span class="gn">M-01</span>考勤管理</div>
    <ul>
      <li><span class="ln">1</span>项目背景<span class="dt"></span></li>
      <li><span class="ln">2</span>相关资料<span class="dt"></span></li>
      <li><span class="ln">3</span>参与人<span class="dt"></span></li>
      <li><span class="ln">4</span>功能模块<span class="dt"></span></li>
      <li><span class="ln">5</span>功能模块树<span class="dt"></span></li>
      <li><span class="ln">6</span>流程图<span class="dt"></span></li>
      <li><span class="ln">7</span>UML 图<span class="dt"></span></li>
      <li><span class="ln">8</span>数据库设计<span class="dt"></span></li>
      <li><span class="ln">9</span>API 设计<span class="dt"></span></li>
      <li><span class="ln">10</span>关键技术设计<span class="dt"></span></li>
      <li><span class="ln">11</span>排期<span class="dt"></span></li>
    </ul>
  </div>
  <div class="group">
    <div class="group-title"><span class="gn">M-02</span>薪资管理</div>
    <ul>
      <li><span class="ln">1</span>项目背景<span class="dt"></span></li>
      <li><span class="ln">2</span>相关资料<span class="dt"></span></li>
      <li><span class="ln">3</span>参与人<span class="dt"></span></li>
      <li><span class="ln">4</span>功能模块<span class="dt"></span></li>
      <li><span class="ln">5</span>功能模块树<span class="dt"></span></li>
      <li><span class="ln">6</span>流程图<span class="dt"></span></li>
      <li><span class="ln">7</span>UML 图<span class="dt"></span></li>
      <li><span class="ln">8</span>数据库设计<span class="dt"></span></li>
      <li><span class="ln">9</span>API 设计<span class="dt"></span></li>
      <li><span class="ln">10</span>关键技术设计<span class="dt"></span></li>
      <li><span class="ln">11</span>排期<span class="dt"></span></li>
    </ul>
  </div>
</section>
<!-- ====================== MODULE 01: 考勤管理 ====================== -->
<section class="module-section">
  <div class="module-banner">
    <div class="left">
      <span class="code">M-01 / §6</span>
      <span class="title">考勤管理</span>
    </div>
    <div class="right">ATTENDANCE MANAGEMENT</div>
  </div>
  <h2 class="sec"><span class="sn">1</span>项目背景</h2>
  <p>本模块来源于 HRMS（人资管理系统）产品规格说明书第 6 部分——考勤管理。当前公司考勤依赖纸质签到与 Excel 统计，存在打卡数据分散、请假审批不规范、统计效率低、考勤数据无法直接驱动薪资核算等问题。</p>
  <p>本模块旨在实现考勤规则配置、网页打卡、请假流程、月度统计与可视化的全流程线上化，为薪资核算模块提供准确的考勤数据支撑，并为 HR 与部门主管提供实时考勤洞察。</p>
  <h2 class="sec"><span class="sn">2</span>相关资料</h2>
  <ul class="bullets">
    <li>人资管理系统（HRMS）详细产品规格说明书 §6 考勤管理</li>
    <li>HRMS-员工档案管理系分（数据基础：员工 / 部门 / 职位）</li>
    <li>AntV 图表库官方文档（折线图 / 饼图 / 柱状图 / 日历图）</li>
  </ul>
  <h2 class="sec"><span class="sn">3</span>参与人</h2>
  <div class="participants">
    <div class="p"><div class="r">PM</div><div class="n">产品经理</div></div>
    <div class="p"><div class="r">UI</div><div class="n">设计师</div></div>
    <div class="p"><div class="r">FE</div><div class="n">前端工程师</div></div>
    <div class="p"><div class="r">BE</div><div class="n">后端工程师</div></div>
    <div class="p"><div class="r">QA</div><div class="n">测试工程师</div></div>
  </div>
  <h2 class="sec"><span class="sn">4</span>功能模块</h2>
  <p>本模块核心功能包括：</p>
  <ul class="bullets">
    <li><strong>考勤规则配置</strong>：考勤组定义（适用人员 / 班次类型 / 上下班时间 / 弹性范围 / 迟到早退阈值）、工作日设置（标准工作日 / 休息日 / 法定节假日）</li>
    <li><strong>打卡功能</strong>：网页端打卡、IP 白名单与 GPS 定位校验、上下班打卡状态判定（正常 / 迟到 / 早退 / 缺卡 / 旷工）、补卡申请</li>
    <li><strong>请假管理</strong>：请假类型（年假 / 病假 / 事假 / 婚假 / 产假 / 丧假 / 调休）、假期余额自动计算（年假按工龄、调休按加班累计）、请假申请与多级审批流</li>
    <li><strong>考勤统计</strong>：个人维度统计（出勤 / 迟到 / 早退 / 旷工 / 请假 / 加班 / 年假余额）、部门维度统计（出勤率 / 迟到率 / 请假率）、AntV 数据可视化</li>
  </ul>
  <h2 class="sec"><span class="sn">5</span>功能模块树</h2>
<pre>考勤管理
├── 考勤规则配置
│   ├── 考勤组定义
│   ├── 考勤组成员管理
│   ├── 工作日设置
│   └── 班次配置（固定班/弹性班/排班制）
├── 打卡功能
│   ├── 网页端打卡
│   ├── IP/GPS 校验
│   ├── 打卡规则
│   ├── 打卡状态判定
│   └── 补卡申请
├── 请假管理
│   ├── 请假类型配置
│   ├── 假期余额计算（年假/调休）
│   ├── 请假申请
│   ├── 审批流路由
│   └── 余额扣减与回滚
└── 考勤统计
    ├── 个人维度统计
    ├── 部门维度统计
    ├── AntV 可视化报表
    └── 月度定时归档</pre>
  <h2 class="sec"><span class="sn">6</span>流程图</h2>
  <h3 class="sub">6-1 打卡流程</h3>
<pre>员工进入打卡页面
     │
     ▼
系统读取员工所属考勤组
     │
     ├── 未配置考勤组 → 提示"未配置考勤组，请联系HR"
     │
     ▼
校验当日是否为工作日
     │
     ├── 休息日/法定节假日 → 提示"今日非工作日"
     │
     ▼
校验打卡位置（IP 白名单 / GPS 范围）
     │
     ├── 不在范围 → 提示"不在打卡范围"
     │
     ▼
记录打卡时间 / IP / GPS
     │
     ▼
根据班次与阈值判定打卡状态
     │
     ├── 上班打卡 ≤ 规定时间            → 正常
     ├── 规定时间 &lt; 打卡 ≤ 规定+阈值     → 迟到
     ├── 打卡 &gt; 规定+阈值              → 旷工半天
     ├── 下班打卡 ≥ 规定时间            → 正常
     ├── 规定-阈值 ≤ 打卡 &lt; 规定        → 早退
     ├── 打卡 &lt; 规定-阈值              → 旷工半天
     └── 当日无打卡记录                 → 缺勤
     │
     ▼
写入 attendance_record，刷新当日缓存状态</pre>
  <h3 class="sub">6-2 请假申请流程</h3>
<pre>员工发起请假申请
     │
     ▼
系统校验请假类型与余额
     │
     ├── 余额不足（年假/调休）→ 提示"余额不足"
     ├── 需上传证明（病假& gt;1天/婚假/产假）→ 校验附件
     │
     ▼
系统按规则计算请假天数（支持 0.5 天）
     │
     ▼
按类型 + 天数路由审批流
     │
     ├── 年假/调休 ≤ 3 天  → 直接上级
     ├── 年假/调休 &gt; 3 天  → 直接上级 → 部门负责人
     ├── 病假/事假 ≤ 1 天  → 直接上级
     ├── 病假/事假 &gt; 1 天  → 直接上级 → 部门负责人
     └── 婚假/产假/丧假    → 直接上级 → HR 备案
     │
     ▼
审批通过 → 扣减假期余额、写入考勤统计
审批拒绝 → 释放预扣减余额
审批撤回 → 释放预扣减余额</pre>
  <h3 class="sub">6-3 月度考勤统计流程</h3>
<pre>定时任务触发（每月 1 日凌晨 02:00）
     │
     ▼
扫描上月所有在职员工的打卡记录
     │
     ▼
按规则汇总个人维度
     │
     ├── 应出勤天数 = 当月工作日数
     ├── 实际出勤天数 = 有打卡记录的天数
     ├── 迟到次数 / 早退次数 / 旷工天数 / 请假天数
     └── 加班时长、年假余额
     │
     ▼
汇总部门维度
     │
     ├── 部门出勤率 = 实际出勤天数 / 应出勤天数
     ├── 部门迟到率 = 迟到人次 / 部门人数
     └── 部门请假率 = 请假天数 / 应出勤天数
     │
     ▼
生成 AntV 可视化数据集并缓存
     │
     ▼
写入 attendance_stat，归档上月打卡明细</pre>
  <h2 class="sec"><span class="sn">7</span>UML 图</h2>
  <h3 class="sub">7-1 考勤管理核心领域模型</h3>
<pre>@startuml
class AttendanceGroup {
  - id: Long
  - name: String                 "考勤组名称"
  - shiftType: ShiftType         "班次类型"
  - workStart: LocalTime         "上班时间"
  - workEnd: LocalTime           "下班时间"
  - restStart: LocalTime
  - restEnd: LocalTime
  - flexibleStart: LocalTime     "弹性最早打卡"
  - flexibleEnd: LocalTime       "弹性最晚打卡"
  - lateThreshold: Integer       "迟到阈值（分钟）"
  - earlyLeaveThreshold: Integer
  - status: Integer              "0=禁用 1=启用"
}
class AttendanceGroupMember {
  - id: Long
  - groupId: Long
  - memberType: MemberType       "1=部门 2=职位 3=个人"
  - memberId: Long
}
class WorkSchedule {
  - id: Long
  - groupId: Long
  - scheduleDate: LocalDate
  - dayType: DayType             "1=工作日 2=休息日 3=节假日"
}
class AttendanceRecord {
  - id: Long
  - employeeId: Long
  - groupId: Long
  - recordDate: LocalDate
  - clockInTime: LocalDateTime
  - clockOutTime: LocalDateTime
  - clockInStatus: ClockStatus
  - clockOutStatus: ClockStatus
  - clockInIp: String
  - clockOutIp: String
  - clockInGps: String
  - clockOutGps: String
}
class LeaveType {
  - id: Long
  - name: String                 "年假/病假/事假/..."
  - code: String
  - hasBalance: Boolean
  - needProof: Boolean
}
class LeaveBalance {
  - id: Long
  - employeeId: Long
  - leaveTypeId: Long
  - year: Integer
  - totalDays: BigDecimal
  - usedDays: BigDecimal
  - remainingDays: BigDecimal
}
class LeaveRequest {
  - id: Long
  - requestNo: String
  - employeeId: Long
  - leaveTypeId: Long
  - startTime: LocalDateTime
  - endTime: LocalDateTime
  - startPeriod: Period
  - endPeriod: Period
  - days: BigDecimal
  - reason: String
  - attachmentUrl: String
  - handoverId: Long
  - status: ApprovalStatus
}
class AttendanceStat {
  - id: Long
  - employeeId: Long
  - statMonth: String            "yyyy-MM"
  - shouldAttendDays: Integer
  - actualAttendDays: Integer
  - lateCount: Integer
  - earlyLeaveCount: Integer
  - absenceDays: BigDecimal
  - leaveDays: BigDecimal
  - overtimeHours: BigDecimal
  - annualLeaveRemaining: BigDecimal
}
enum ShiftType { FIXED  FLEXIBLE  SCHEDULED }
enum DayType    { WORKDAY  REST  HOLIDAY }
enum ClockStatus{ NORMAL  LATE  EARLY_LEAVE  MISSING  ABSENCE }
AttendanceGroup *-- AttendanceGroupMember
AttendanceGroup *-- WorkSchedule
AttendanceRecord --> AttendanceGroup
LeaveBalance --> LeaveType
LeaveRequest --> LeaveType
AttendanceStat --> Employee
@enduml</pre>
  <h3 class="sub">7-2 打卡时序图</h3>
<pre>@startuml
actor Employee as emp
participant "前端" as fe
participant "网关/鉴权" as gateway
participant "考勤服务" as attSvc
participant "规则校验服务" as ruleSvc
database "MySQL" as db
database "Redis" as cache
emp -> fe : 点击"打卡"
fe -> gateway : POST /api/v1/attendance/clock
gateway -> attSvc : 转发打卡请求(含IP/GPS)
attSvc -> cache : 查询员工考勤组(预热)
cache --> attSvc : 返回考勤组配置
attSvc -> ruleSvc : 校验打卡条件(工作日/IP/GPS)
ruleSvc --> attSvc : 校验结果
alt 校验通过
    attSvc -> db : INSERT/UPDATE attendance_record
    attSvc -> cache : 刷新当日打卡状态
    attSvc --> fe : 返回打卡成功+状态
    fe --> emp : 展示打卡结果与状态
else 校验失败
    attSvc --> fe : 返回失败原因
    fe --> emp : 提示失败原因
end
@enduml</pre>
  <h3 class="sub">7-3 请假申请时序图</h3>
<pre>@startuml
actor Employee as emp
participant "前端" as fe
participant "考勤服务" as attSvc
participant "审批引擎" as wf
participant "通知服务" as notifySvc
database "MySQL" as db
database "Redis" as cache
emp -> fe : 提交请假申请
fe -> attSvc : POST /api/v1/leaves
attSvc -> cache : 查询假期余额
cache --> attSvc : 返回余额
alt 余额充足 & 附件齐全
    attSvc -> db : 预扣减假期余额(乐观锁)
    attSvc -> db : INSERT leave_request
    attSvc -> wf : 创建审批流程(按规则路由)
    wf --> attSvc : 返回流程ID
    attSvc -> notifySvc : 通知审批人
    attSvc --> fe : 申请成功
    fe --> emp : 展示审批进度
else 余额不足
    attSvc --> fe : 余额不足
    fe --> emp : 提示调整天数
end
note over wf, db
  审批通过 → 确认扣减余额
  审批拒绝/撤回 → 回滚预扣减余额
end note
@enduml</pre>
  <h3 class="sub">7-4 月度考勤统计时序图</h3>
<pre>@startuml
participant "定时任务调度" as scheduler
participant "考勤统计服务" as statSvc
participant "考勤服务" as attSvc
database "MySQL" as db
database "Redis" as cache
scheduler -> statSvc : 触发月度统计(每月1日02:00)
statSvc -> attSvc : 获取上月所有员工打卡记录
attSvc -> db : SELECT attendance_record WHERE month=last
db --> attSvc : 返回记录列表
attSvc --> statSvc : 返回打卡数据
statSvc -> statSvc : 汇总个人维度(出勤/迟到/早退/旷工/请假/加班)
statSvc -> statSvc : 汇总部门维度(出勤率/迟到率/请假率)
statSvc -> db : 批量 UPSERT attendance_stat
statSvc -> cache : 缓存 AntV 可视化数据集
statSvc --> scheduler : 统计完成，归档上月数据
@enduml</pre>
  <h2 class="sec"><span class="sn">8</span>数据库设计</h2>
  <h3 class="sub">8-1 考勤组表 attendance_group</h3>
<pre>CREATE TABLE IF NOT EXISTS `attendance_group` (
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                    VARCHAR(64)     NOT NULL COMMENT '考勤组名称',
    `shift_type`              TINYINT         NOT NULL COMMENT '班次类型：1=固定班 2=弹性班 3=排班制',
    `work_start`              TIME            NOT NULL COMMENT '上班时间',
    `work_end`                TIME            NOT NULL COMMENT '下班时间',
    `rest_start`              TIME                     COMMENT '午休开始',
    `rest_end`                TIME                     COMMENT '午休结束',
    `flexible_start`          TIME                     COMMENT '弹性最早打卡',
    `flexible_end`            TIME                     COMMENT '弹性最晚打卡',
    `late_threshold`          INT             NOT NULL DEFAULT 15 COMMENT '迟到阈值（分钟）',
    `early_leave_threshold`   INT             NOT NULL DEFAULT 15 COMMENT '早退阈值（分钟）',
    `status`                  TINYINT         NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    `create_time`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`              TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤组表';</pre>
  <h3 class="sub">8-2 考勤组成员表 attendance_group_member</h3>
<pre>CREATE TABLE IF NOT EXISTS `attendance_group_member` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `group_id`     BIGINT UNSIGNED NOT NULL COMMENT '考勤组ID',
    `member_type`  TINYINT         NOT NULL COMMENT '成员类型：1=部门 2=职位 3=个人',
    `member_id`    BIGINT UNSIGNED NOT NULL COMMENT '成员ID',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_member` (`member_type`, `member_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤组成员表';</pre>
  <h3 class="sub">8-3 工作日设置表 work_schedule</h3>
<pre>CREATE TABLE IF NOT EXISTS `work_schedule` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `group_id`       BIGINT UNSIGNED NOT NULL COMMENT '考勤组ID',
    `schedule_date`  DATE            NOT NULL COMMENT '日期',
    `day_type`       TINYINT         NOT NULL COMMENT '1=标准工作日 2=休息日 3=法定节假日',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_date` (`group_id`, `schedule_date`),
    KEY `idx_schedule_date` (`schedule_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工作日设置表';</pre>
  <h3 class="sub">8-4 打卡记录表 attendance_record</h3>
<pre>CREATE TABLE IF NOT EXISTS `attendance_record` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `employee_id`        BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
    `group_id`           BIGINT UNSIGNED NOT NULL COMMENT '考勤组ID',
    `record_date`        DATE            NOT NULL COMMENT '打卡日期',
    `clock_in_time`      DATETIME                 COMMENT '上班打卡时间',
    `clock_out_time`     DATETIME                 COMMENT '下班打卡时间',
    `clock_in_status`    TINYINT                 COMMENT '上班状态：1=正常 2=迟到 3=缺卡 4=旷工',
    `clock_out_status`   TINYINT                 COMMENT '下班状态：1=正常 2=早退 3=缺卡 4=旷工',
    `clock_in_ip`        VARCHAR(64)              COMMENT '上班打卡IP',
    `clock_out_ip`       VARCHAR(64)              COMMENT '下班打卡IP',
    `clock_in_gps`       VARCHAR(64)              COMMENT '上班打卡GPS',
    `clock_out_gps`      VARCHAR(64)              COMMENT '下班打卡GPS',
    `create_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_date` (`employee_id`, `record_date`),
    KEY `idx_group_date` (`group_id`, `record_date`),
    KEY `idx_record_date` (`record_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '打卡记录表';</pre>
  <h3 class="sub">8-5 请假类型表 leave_type</h3>
<pre>CREATE TABLE IF NOT EXISTS `leave_type` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(32)     NOT NULL COMMENT '类型名称：年假/病假/事假/婚假/产假/丧假/调休',
    `code`         VARCHAR(32)     NOT NULL COMMENT '类型编码',
    `has_balance`  TINYINT         NOT NULL DEFAULT 0 COMMENT '是否管理余额：0=否 1=是',
    `need_proof`   TINYINT         NOT NULL DEFAULT 0 COMMENT '是否需要证明材料',
    `sort_order`   INT             NOT NULL DEFAULT 0,
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '请假类型表';</pre>
  <h3 class="sub">8-6 假期余额表 leave_balance</h3>
<pre>CREATE TABLE IF NOT EXISTS `leave_balance` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `employee_id`     BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
    `leave_type_id`   BIGINT UNSIGNED NOT NULL COMMENT '请假类型ID',
    `year`            SMALLINT        NOT NULL COMMENT '年份',
    `total_days`      DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '总额度',
    `used_days`       DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '已使用',
    `remaining_days`  DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '剩余',
    `version`         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_type_year` (`employee_id`, `leave_type_id`, `year`),
    KEY `idx_employee_id` (`employee_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '假期余额表';</pre>
  <h3 class="sub">8-7 请假申请表 leave_request</h3>
<pre>CREATE TABLE IF NOT EXISTS `leave_request` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `request_no`      VARCHAR(32)     NOT NULL COMMENT '申请单号',
    `employee_id`     BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
    `leave_type_id`   BIGINT UNSIGNED NOT NULL COMMENT '请假类型ID',
    `start_time`      DATETIME        NOT NULL COMMENT '开始时间',
    `end_time`        DATETIME        NOT NULL COMMENT '结束时间',
    `start_period`    TINYINT         NOT NULL COMMENT '开始时段：1=上午 2=下午',
    `end_period`      TINYINT         NOT NULL COMMENT '结束时段：1=上午 2=下午',
    `days`            DECIMAL(6,1)    NOT NULL COMMENT '请假天数（支持0.5天）',
    `reason`          VARCHAR(512)    NOT NULL COMMENT '请假事由',
    `attachment_url`  VARCHAR(512)             COMMENT '附件URL',
    `handover_id`     BIGINT UNSIGNED          COMMENT '工作交接人ID',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '0=草稿 1=审批中 2=已批准 3=已拒绝 4=已撤回',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_request_no` (`request_no`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '请假申请表';</pre>
  <h3 class="sub">8-8 考勤统计表 attendance_stat</h3>
<pre>CREATE TABLE IF NOT EXISTS `attendance_stat` (
    `id`                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `employee_id`              BIGINT UNSIGNED NOT NULL,
    `stat_month`               CHAR(7)         NOT NULL COMMENT '统计月份，如 2024-07',
    `should_attend_days`       INT             NOT NULL COMMENT '应出勤天数',
    `actual_attend_days`       INT             NOT NULL COMMENT '实际出勤天数',
    `late_count`               INT             NOT NULL DEFAULT 0 COMMENT '迟到次数',
    `early_leave_count`        INT             NOT NULL DEFAULT 0 COMMENT '早退次数',
    `absence_days`             DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '旷工天数',
    `leave_days`               DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '请假天数',
    `overtime_hours`           DECIMAL(6,1)    NOT NULL DEFAULT 0 COMMENT '加班时长',
    `annual_leave_remaining`   DECIMAL(6,1)             COMMENT '年假余额',
    `create_time`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_month` (`employee_id`, `stat_month`),
    KEY `idx_stat_month` (`stat_month`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤统计表';</pre>
  <h2 class="sec"><span class="sn">9</span>API 设计</h2>
  <div class="api">
    <div class="head">
      <span class="method">GET</span>
      <span class="path">/api/v1/attendance/groups</span>
      <span class="desc">查询考勤组列表</span>
    </div>
    <div class="body">
      <h4>请求参数</h4>
      <div class="table-wrap">
        <table>
          <tr><th>参数</th><th>类型</th><th>必填</th><th>描述</th></tr>
          <tr><td>keyword</td><td>String</td><td>否</td><td>考勤组名称模糊匹配</td></tr>
          <tr><td>status</td><td>Integer</td><td>否</td><td>0=禁用 1=启用</td></tr>
          <tr><td>page</td><td>Integer</td><td>否</td><td>页码，默认1</td></tr>
          <tr><td>size</td><td>Integer</td><td>否</td><td>每页条数，默认20</td></tr>
        </table>
      </div>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">POST</span>
      <span class="path">/api/v1/attendance/groups</span>
      <span class="desc">创建考勤组</span>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">POST</span>
      <span class="path">/api/v1/attendance/clock</span>
      <span class="desc">员工打卡</span>
    </div>
    <div class="body">
      <h4>请求参数</h4>
      <div class="table-wrap">
        <table>
          <tr><th>参数</th><th>类型</th><th>必填</th><th>描述</th></tr>
          <tr><td>clockType</td><td>Integer</td><td class="req">是</td><td>1=上班打卡 2=下班打卡</td></tr>
          <tr><td>ipAddress</td><td>String</td><td class="req">是</td><td>客户端IP（由网关透传）</td></tr>
          <tr><td>gpsLocation</td><td>String</td><td>否</td><td>GPS定位（经纬度），可选</td></tr>
          <tr><td>deviceInfo</td><td>String</td><td>否</td><td>设备信息</td></tr>
        </table>
      </div>
      <h4>响应示例</h4>
<pre>{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": 8848,
    "clockTime": "2024-07-09T09:02:13",
    "clockStatus": "LATE",
    "statusDesc": "迟到"
  }
}</pre>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">POST</span>
      <span class="path">/api/v1/leaves</span>
      <span class="desc">提交请假申请</span>
    </div>
    <div class="body">
      <h4>请求参数</h4>
      <div class="table-wrap">
        <table>
          <tr><th>参数</th><th>类型</th><th>必填</th><th>描述</th></tr>
          <tr><td>leaveTypeId</td><td>Long</td><td class="req">是</td><td>请假类型ID</td></tr>
          <tr><td>startTime</td><td>DateTime</td><td class="req">是</td><td>开始时间</td></tr>
          <tr><td>endTime</td><td>DateTime</td><td class="req">是</td><td>结束时间</td></tr>
          <tr><td>startPeriod</td><td>Integer</td><td class="req">是</td><td>开始时段：1=上午 2=下午</td></tr>
          <tr><td>endPeriod</td><td>Integer</td><td class="req">是</td><td>结束时段：1=上午 2=下午</td></tr>
          <tr><td>reason</td><td>String</td><td class="req">是</td><td>请假事由</td></tr>
          <tr><td>attachmentUrl</td><td>String</td><td>条件必填</td><td>病假/婚假/产假需上传证明</td></tr>
          <tr><td>handoverId</td><td>Long</td><td>否</td><td>工作交接人ID</td></tr>
        </table>
      </div>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">GET</span>
      <span class="path">/api/v1/leaves/balances</span>
      <span class="desc">查询假期余额</span>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">GET</span>
      <span class="path">/api/v1/attendance/stats/personal</span>
      <span class="desc">个人维度考勤统计</span>
    </div>
    <div class="body">
      <h4>请求参数</h4>
      <div class="table-wrap">
        <table>
          <tr><th>参数</th><th>类型</th><th>必填</th><th>描述</th></tr>
          <tr><td>employeeId</td><td>Long</td><td>否</td><td>不传则取当前登录用户</td></tr>
          <tr><td>statMonth</td><td>String</td><td class="req">是</td><td>统计月份，如 2024-07</td></tr>
        </table>
      </div>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">GET</span>
      <span class="path">/api/v1/attendance/stats/visualization</span>
      <span class="desc">考勤可视化数据（AntV）</span>
    </div>
    <div class="body">
      <h4>响应数据集</h4>
      <div class="table-wrap">
        <table>
          <tr><th>图表</th><th>图表类型</th><th>数据内容</th><th>适用角色</th></tr>
          <tr><td>部门出勤率趋势</td><td>折线图</td><td>近 6 个月各部门出勤率变化</td><td>HR、部门主管</td></tr>
          <tr><td>请假类型分布</td><td>饼图/环形图</td><td>当月各请假类型占比</td><td>HR</td></tr>
          <tr><td>迟到早退排行</td><td>柱状图</td><td>部门维度迟到早退人次对比</td><td>HR、部门主管</td></tr>
          <tr><td>考勤日历图</td><td>折线/柱状图</td><td>每日出勤状态</td><td>普通员工、HR</td></tr>
        </table>
      </div>
    </div>
  </div>
  <h2 class="sec"><span class="sn">10</span>关键技术设计</h2>
  <h3 class="sub">10-1 打卡防作弊方案</h3>
  <ul class="bullets">
    <li>支持配置 IP 白名单：员工打卡时由网关透传真实 IP，服务端校验是否在白名单网段内</li>
    <li>支持配置 GPS 定位范围：前端通过 H5 <code class="inline">navigator.geolocation</code> 获取定位，服务端按预定义圆形区域（中心点 + 半径）判定</li>
    <li>IP 与 GPS 至少满足一项，否则拒绝打卡并记录异常日志</li>
    <li>设备指纹辅助识别代打卡风险（UA + IP + 时序特征）</li>
  </ul>
  <h3 class="sub">10-2 假期余额并发扣减方案</h3>
  <p>采用乐观锁 + 状态机双重保障，避免并发请假导致余额超扣：</p>
  <ul class="bullets">
    <li>提交请假申请时执行预扣减：<code class="inline">UPDATE leave_balance SET used_days = used_days + ?, remaining_days = remaining_days - ?, version = version + 1 WHERE id = ? AND version = ?</code></li>
    <li>审批通过 → 确认扣减，状态置为 <code class="inline">CONFIRMED</code></li>
    <li>审批拒绝 / 撤回 → 回滚预扣减，状态置为 <code class="inline">ROLLED_BACK</code></li>
    <li>乐观锁失败时返回 <code class="inline">40909 CONFLICT</code>，前端提示"余额已变化，请刷新重试"</li>
  </ul>
  <h3 class="sub">10-3 年假余额自动计算规则</h3>
<pre>入职工龄 &lt; 1 年              → 0 天
入职满 1 年不满 10 年         → 5 天/年
入职满 10 年不满 20 年        → 10 天/年
入职满 20 年                  → 15 天/年
当年入职按剩余月份折算：
  折算天数 = (12 - 入职月份 + 1) / 12 × 对应天数
  四舍五入至 0.5 天
调休：加班时长 1:1 转换，加班当月及次月有效，过期清零</pre>
  <h3 class="sub">10-4 月度统计定时任务方案</h3>
  <ul class="bullets">
    <li>调度：RabbitMQ 延迟队列 + XXL-JOB 分布式调度，每月 1 日 02:00 触发</li>
    <li>分片：按 employeeId 取模分片，单批 200 人，并发度 4，避免长事务</li>
    <li>幂等：<code class="inline">attendance_stat</code> 表 <code class="inline">uk_emp_month</code> 唯一索引 + UPSERT，支持重跑</li>
    <li>失败重试：单员工失败记录到 <code class="inline">stat_failed_log</code>，人工补跑</li>
    <li>可视化数据集生成后写入 Redis，TTL 7 天，HR 看板直接读取</li>
  </ul>
  <h3 class="sub">10-5 数据可视化方案（AntV）</h3>
  <ul class="bullets">
    <li>折线图：@antv/g2 <code class="inline">Line</code>，展示部门出勤率 6 个月趋势</li>
    <li>饼图：@antv/g2 <code class="inline">Pie</code>，展示当月请假类型占比</li>
    <li>柱状图：@antv/g2 <code class="inline">Column</code>，展示部门迟到早退人次对比</li>
    <li>日历图：@antv/g2 <code class="inline">Calendar</code>，展示每日出勤状态</li>
    <li>统一封装 <code class="inline">&lt;Chart type="..." data={...} /&gt;</code> 组件，按角色权限控制可见图表</li>
  </ul>
  <h2 class="sec"><span class="sn">11</span>排期</h2>
  <div class="schedule">
    <div class="schedule-row"><div class="idx">01</div><div class="stage">需求评审<small>评审产品规格，确认考勤规则与审批流路由</small></div><div class="dur">1 天</div></div>
    <div class="schedule-row"><div class="idx">02</div><div class="stage">技术方案<small>完成系分评审，确认数据库与接口方案</small></div><div class="dur">2 天</div></div>
    <div class="schedule-row"><div class="idx">03</div><div class="stage">数据库开发<small>建表、索引优化、初始化数据（节假日/请假类型）</small></div><div class="dur">2 天</div></div>
    <div class="schedule-row"><div class="idx">04</div><div class="stage">后端开发<small>考勤组、打卡、请假、统计、定时任务</small></div><div class="dur">9 天</div></div>
    <div class="schedule-row"><div class="idx">05</div><div class="stage">前端开发<small>打卡页、请假申请、统计看板、AntV 图表</small></div><div class="dur">8 天</div></div>
    <div class="schedule-row"><div class="idx">06</div><div class="stage">联调测试<small>前后端联调、并发扣减测试、防作弊测试</small></div><div class="dur">4 天</div></div>
    <div class="schedule-row"><div class="idx">07</div><div class="stage">回归上线<small>全量回归、预发验证、正式上线</small></div><div class="dur">2 天</div></div>
  </div>
  <div class="schedule-total">
    <span>总预估工期</span>
    <span class="v">28 个工作日</span>
  </div>
</section>
<!-- ====================== MODULE 02: 薪资管理 ====================== -->
<section class="module-section">
  <div class="module-banner">
    <div class="left">
      <span class="code">M-02 / §7</span>
      <span class="title">薪资管理</span>
    </div>
    <div class="right">SALARY MANAGEMENT</div>
  </div>
  <h2 class="sec"><span class="sn">1</span>项目背景</h2>
  <p>本模块来源于 HRMS 产品规格说明书第 7 部分——薪资管理。当前公司薪资核算依赖 Excel 模板与手工计算，存在算薪效率低、考勤数据未打通、异常难发现、工资条分发不透明等问题。</p>
  <p>本模块旨在建立统一的薪资账套与员工薪资档案，实现月度薪资自动核算、异常检测、财务审批与工资条线上分发，并基于 AntV 提供薪资成本可视化看板，支撑 HR 与财务的精细化运营。</p>
  <h2 class="sec"><span class="sn">2</span>相关资料</h2>
  <ul class="bullets">
    <li>人资管理系统（HRMS）详细产品规格说明书 §7 薪资管理</li>
    <li>HRMS-员工档案管理系分（员工薪资档案字段定义）</li>
    <li>考勤管理模块系分（考勤统计数据来源）</li>
    <li>国家税务总局累计预扣法公告（个税计算规则）</li>
  </ul>
  <h2 class="sec"><span class="sn">3</span>参与人</h2>
  <div class="participants">
    <div class="p"><div class="r">PM</div><div class="n">产品经理</div></div>
    <div class="p"><div class="r">UI</div><div class="n">设计师</div></div>
    <div class="p"><div class="r">FE</div><div class="n">前端工程师</div></div>
    <div class="p"><div class="r">BE</div><div class="n">后端工程师</div></div>
    <div class="p"><div class="r">QA</div><div class="n">测试工程师</div></div>
  </div>
  <h2 class="sec"><span class="sn">4</span>功能模块</h2>
  <p>本模块核心功能包括：</p>
  <ul class="bullets">
    <li><strong>薪资账套</strong>：账套定义（名称 / 适用范围 / 生效日期 / 工资项目）、工资项目类型（固定收入 / 变动收入 / 考勤扣款 / 社保扣除 / 公积金扣除 / 个税）、典型账套示例（标准职员）</li>
    <li><strong>员工薪资设置</strong>：薪资档案（适用账套 / 基本工资 / 各项津贴基数 / 社保公积金基数 / 调薪历史）、试用期薪资（80%–100% 比例）</li>
    <li><strong>月度薪资核算</strong>：核算流程（创建批次 → 自动计算 → 异常检测 → HR 确认 → 财务审批 → 发放）、批次状态机、异常检测规则、AntV 数据可视化</li>
    <li><strong>工资条</strong>：员工查看规则（二次验证）、工资条内容示例、个人薪资趋势图（AntV 折线图）</li>
  </ul>
  <h2 class="sec"><span class="sn">5</span>功能模块树</h2>
<pre>薪资管理
├── 薪资账套（工资模板）
│   ├── 账套定义
│   ├── 工资项目类型
│   ├── 典型账套示例
│   └── 账套启用/停用
├── 员工薪资设置
│   ├── 薪资档案
│   ├── 试用期薪资比例
│   └── 调薪历史
├── 月度薪资核算
│   ├── 核算流程
│   ├── 薪资批次状态机
│   ├── 异常检测规则
│   ├── 财务审批
│   └── AntV 数据可视化
└── 工资条
    ├── 员工查看规则
    ├── 工资条内容
    ├── 二次验证
    └── 个人薪资趋势图</pre>
  <h2 class="sec"><span class="sn">6</span>流程图</h2>
  <h3 class="sub">6-1 月度薪资核算流程</h3>
<pre>HR 创建薪资批次（选择月份 / 范围）
     │
     ▼
状态：草稿 ──&gt; 计算中
     │
     ▼
系统拉取当月考勤统计、加班时长、请假天数
     │
     ▼
按账套公式逐项计算：
     ├── 固定收入（直接取值）
     ├── 变动收入（公式计算：绩效 / 加班费）
     ├── 考勤扣款（迟到 / 请假）
     ├── 社保扣除（基数 × 比例）
     ├── 公积金扣除（基数 × 12%）
     └── 个税（累计预扣法）
     │
     ▼
异常检测
     │
     ├── 请假 &gt; 15 天     → 黄色预警
     ├── 加班费 &gt; 50 小时 → 黄色预警
     ├── 较上月变动 &gt; 30% → 红色预警（需确认）
     └── 未设薪资档案     → 红色阻断（无法计算）
     │
     ▼
状态：计算中 ──&gt; 待确认
     │
     ▼
HR 预览 / 调整 / 提交审批 ──&gt; 审批中
     │
     ▼
财务专员审批 ── [老板]（金额超阈值时二审）
     │
     ├── 驳回 → 修改后重新提交
     └── 通过 → 已通过
     │
     ▼
实际发放后标记已发放 ──&gt; 工资条对员工可见
     │
     ▼
状态归档</pre>
  <h3 class="sub">6-2 工资条查看流程</h3>
<pre>员工进入"我的薪资"
     │
     ▼
选择月份查看工资条
     │
     ▼
触发二次验证（短信验证码 / 密码）
     │
     ├── 验证失败 → 拒绝查看
     └── 验证通过 → 返回工资条详情
     │
     ▼
展示工资项目明细 + 个人薪资趋势图（AntV 折线图，近6月实发）</pre>
  <h3 class="sub">6-3 试用期薪资计算流程</h3>
<pre>读取员工在职状态
     │
     ├── 状态 = 试用期
     │     │
     │     ▼
     │   基本工资 × 试用期比例（80%–100%）
     │   津贴同步按比例折算
     │   社保公积金按全额基数缴纳
     │
     └── 状态 = 正式
          │
          ▼
        全额基本工资 + 全额津贴
        社保公积金按全额基数缴纳</pre>
  <h2 class="sec"><span class="sn">7</span>UML 图</h2>
  <h3 class="sub">7-1 薪资管理核心领域模型</h3>
<pre>@startuml
class SalaryAccount {
  - id: Long
  - name: String                 "账套名称"
  - scopeType: ScopeType         "1=部门 2=职位 3=职级"
  - scopeValue: String
  - effectiveDate: LocalDate     "生效日期"
  - status: Integer              "0=停用 1=启用"
}
class SalaryItem {
  - id: Long
  - accountId: Long
  - name: String                 "项目名称"
  - itemType: SalaryItemType
  - formula: String              "计算公式/规则"
  - sortOrder: Integer
}
enum SalaryItemType {
  FIXED_INCOME        "固定收入"
  VARIABLE_INCOME     "变动收入"
  ATTENDANCE_DEDUCT   "考勤扣款"
  SOCIAL_INSURANCE    "社保扣除"
  HOUSING_FUND        "公积金扣除"
  INCOME_TAX          "个税"
}
class EmployeeSalary {
  - id: Long
  - employeeId: Long
  - accountId: Long               "适用账套"
  - baseSalary: BigDecimal        "基本工资"
  - allowanceBase: BigDecimal     "津贴基数"
  - socialInsuranceBase: BigDecimal
  - housingFundBase: BigDecimal
  - performanceBase: BigDecimal
  - probationRatio: BigDecimal    "试用期比例 0.8~1.0"
  - effectiveDate: LocalDate
}
class SalaryAdjustment {
  - id: Long
  - employeeSalaryId: Long
  - adjustDate: LocalDate
  - oldBaseSalary: BigDecimal
  - newBaseSalary: BigDecimal
  - reason: String
  - operatorId: Long
}
class SalaryBatch {
  - id: Long
  - batchNo: String               "批次编号"
  - month: String                 "yyyy-MM"
  - status: BatchStatus
  - totalCount: Integer
  - totalGross: BigDecimal        "应发总额"
  - totalNet: BigDecimal          "实发总额"
  - createdBy: Long
  - createTime: LocalDateTime
}
enum BatchStatus {
  DRAFT         "草稿"
  CALCULATING   "计算中"
  PENDING_CONFIRM "待确认"
  APPROVING     "审批中"
  APPROVED      "已通过"
  PAID          "已发放"
  REJECTED      "已驳回"
}
class SalaryBatchItem {
  - id: Long
  - batchId: Long
  - employeeId: Long
  - baseSalary: BigDecimal
  - allowance: BigDecimal
  - performance: BigDecimal
  - overtimePay: BigDecimal
  - lateDeduction: BigDecimal
  - leaveDeduction: BigDecimal
  - socialInsurance: BigDecimal
  - housingFund: BigDecimal
  - incomeTax: BigDecimal
  - grossSalary: BigDecimal       "应发"
  - netSalary: BigDecimal         "实发"
  - exceptionLevel: Integer       "0=正常 1=黄色 2=红色"
}
class Payslip {
  - id: Long
  - batchItemId: Long
  - employeeId: Long
  - month: String
  - viewed: Boolean
  - viewTime: LocalDateTime
}
SalaryAccount *-- SalaryItem
EmployeeSalary --> SalaryAccount
EmployeeSalary *-- SalaryAdjustment
SalaryBatch *-- SalaryBatchItem
SalaryBatchItem --> EmployeeSalary
Payslip --> SalaryBatchItem
@enduml</pre>
  <h3 class="sub">7-2 薪资核算时序图</h3>
<pre>@startuml
actor HR as hr
participant "前端" as fe
participant "薪资服务" as salSvc
participant "考勤服务" as attSvc
participant "计算引擎" as calcEngine
participant "异常检测" as ruleSvc
participant "审批引擎" as wf
database "MySQL" as db
database "Redis" as cache
hr -> fe : 创建薪资批次（月份/范围）
fe -> salSvc : POST /api/v1/salary/batches
salSvc -> db : INSERT salary_batch (status=DRAFT)
salSvc --> fe : 返回批次ID
hr -> fe : 点击"开始计算"
fe -> salSvc : POST /api/v1/salary/batches/{id}/calculate
salSvc -> db : UPDATE status=CALCULATING
salSvc -> attSvc : 拉取当月考勤统计
attSvc --> salSvc : 返回考勤数据
salSvc -> cache : 查询员工薪资档案(预热)
cache --> salSvc : 返回档案
salSvc -> calcEngine : 逐项计算（固定/变动/扣款/社保/个税）
calcEngine --> salSvc : 返回计算结果
salSvc -> ruleSvc : 异常检测
ruleSvc --> salSvc : 返回异常标记
salSvc -> db : 批量 INSERT salary_batch_item
salSvc -> db : UPDATE status=PENDING_CONFIRM
salSvc -> cache : 缓存可视化数据集
salSvc --> fe : 计算完成
fe --> hr : 展示预览与异常列表
@enduml</pre>
  <h3 class="sub">7-3 工资条查看时序图</h3>
<pre>@startuml
actor Employee as emp
participant "前端" as fe
participant "网关/鉴权" as gateway
participant "薪资服务" as salSvc
participant "二次验证服务" as verifySvc
database "MySQL" as db
emp -> fe : 选择月份查看工资条
fe -> verifySvc : 请求发送短信验证码
verifySvc --> fe : 验证码已发送
emp -> fe : 输入验证码
fe -> gateway : POST /api/v1/salary/payslips/{month} (含验证码)
gateway -> verifySvc : 校验验证码
verifySvc --> gateway : 校验通过
gateway -> salSvc : 查询工资条
salSvc -> db : SELECT salary_batch_item + payslip
db --> salSvc : 返回工资条数据
salSvc -> db : UPDATE payslip SET viewed=1
salSvc --> fe : 返回工资条明细
fe --> emp : 展示工资项目 + 趋势图
@enduml</pre>
  <h2 class="sec"><span class="sn">8</span>数据库设计</h2>
  <h3 class="sub">8-1 薪资账套表 salary_account</h3>
<pre>CREATE TABLE IF NOT EXISTS `salary_account` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`            VARCHAR(64)     NOT NULL COMMENT '账套名称',
    `scope_type`      TINYINT         NOT NULL COMMENT '适用范围：1=部门 2=职位 3=职级',
    `scope_value`     VARCHAR(128)    NOT NULL COMMENT '范围值（逗号分隔）',
    `effective_date`  DATE            NOT NULL COMMENT '生效日期',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`      TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资账套表';</pre>
  <h3 class="sub">8-2 工资项目表 salary_item</h3>
<pre>CREATE TABLE IF NOT EXISTS `salary_item` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `account_id`   BIGINT UNSIGNED NOT NULL COMMENT '账套ID',
    `name`         VARCHAR(64)     NOT NULL COMMENT '项目名称',
    `item_type`    TINYINT         NOT NULL COMMENT '1=固定收入 2=变动收入 3=考勤扣款 4=社保 5=公积金 6=个税',
    `formula`      VARCHAR(512)             COMMENT '计算公式/规则',
    `sort_order`   INT             NOT NULL DEFAULT 0,
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_account_id` (`account_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工资项目表';</pre>
  <h3 class="sub">8-3 员工薪资档案表 employee_salary</h3>
<pre>CREATE TABLE IF NOT EXISTS `employee_salary` (
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `employee_id`             BIGINT UNSIGNED NOT NULL,
    `account_id`              BIGINT UNSIGNED NOT NULL COMMENT '适用账套ID',
    `base_salary`             DECIMAL(12,2)   NOT NULL COMMENT '基本工资',
    `allowance_base`          DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '津贴基数',
    `social_insurance_base`   DECIMAL(12,2)   NOT NULL COMMENT '社保基数',
    `housing_fund_base`       DECIMAL(12,2)   NOT NULL COMMENT '公积金基数',
    `performance_base`        DECIMAL(12,2)            DEFAULT 0 COMMENT '绩效基数',
    `probation_ratio`         DECIMAL(5,4)    NOT NULL DEFAULT 1.0000 COMMENT '试用期比例 0.8~1.0',
    `effective_date`          DATE            NOT NULL COMMENT '生效日期',
    `create_time`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_id` (`employee_id`),
    KEY `idx_account_id` (`account_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工薪资档案表';</pre>
  <h3 class="sub">8-4 调薪历史表 salary_adjustment</h3>
<pre>CREATE TABLE IF NOT EXISTS `salary_adjustment` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `employee_id`         BIGINT UNSIGNED NOT NULL,
    `adjust_date`         DATE            NOT NULL COMMENT '调薪日期',
    `old_base_salary`     DECIMAL(12,2)   NOT NULL COMMENT '原基本工资',
    `new_base_salary`     DECIMAL(12,2)   NOT NULL COMMENT '新基本工资',
    `reason`              VARCHAR(256)             COMMENT '调薪原因',
    `operator_id`         BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
    `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_adjust_date` (`adjust_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '调薪历史表';</pre>
  <h3 class="sub">8-5 薪资批次表 salary_batch</h3>
<pre>CREATE TABLE IF NOT EXISTS `salary_batch` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `batch_no`       VARCHAR(32)     NOT NULL COMMENT '批次编号',
    `month`          CHAR(7)         NOT NULL COMMENT '薪资月份 yyyy-MM',
    `status`         TINYINT         NOT NULL DEFAULT 0 COMMENT '0=草稿 1=计算中 2=待确认 3=审批中 4=已通过 5=已发放 6=已驳回',
    `total_count`    INT             NOT NULL DEFAULT 0 COMMENT '员工数',
    `total_gross`    DECIMAL(15,2)   NOT NULL DEFAULT 0 COMMENT '应发总额',
    `total_net`      DECIMAL(15,2)   NOT NULL DEFAULT 0 COMMENT '实发总额',
    `created_by`     BIGINT UNSIGNED NOT NULL,
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_no` (`batch_no`),
    UNIQUE KEY `uk_month` (`month`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资批次表';</pre>
  <h3 class="sub">8-6 薪资明细表 salary_batch_item</h3>
<pre>CREATE TABLE IF NOT EXISTS `salary_batch_item` (
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `batch_id`            BIGINT UNSIGNED NOT NULL,
    `employee_id`         BIGINT UNSIGNED NOT NULL,
    `base_salary`         DECIMAL(12,2)   NOT NULL COMMENT '基本工资',
    `allowance`           DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '岗位津贴',
    `performance`         DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '绩效奖金',
    `overtime_pay`        DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '加班费',
    `late_deduction`      DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '迟到扣款',
    `leave_deduction`     DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '请假扣款',
    `social_insurance`    DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '社保扣除',
    `housing_fund`        DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '公积金扣除',
    `income_tax`          DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '个人所得税',
    `gross_salary`        DECIMAL(12,2)   NOT NULL COMMENT '应发工资',
    `net_salary`          DECIMAL(12,2)   NOT NULL COMMENT '实发工资',
    `exception_level`     TINYINT         NOT NULL DEFAULT 0 COMMENT '0=正常 1=黄色预警 2=红色阻断',
    `exception_remark`    VARCHAR(256)             COMMENT '异常说明',
    `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_emp` (`batch_id`, `employee_id`),
    KEY `idx_employee_id` (`employee_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资明细表';</pre>
  <h3 class="sub">8-7 工资条表 payslip</h3>
<pre>CREATE TABLE IF NOT EXISTS `payslip` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `batch_item_id`    BIGINT UNSIGNED NOT NULL COMMENT '薪资明细ID',
    `employee_id`      BIGINT UNSIGNED NOT NULL,
    `month`            CHAR(7)         NOT NULL,
    `viewed`           TINYINT         NOT NULL DEFAULT 0 COMMENT '0=未查看 1=已查看',
    `view_time`        DATETIME                 COMMENT '首次查看时间',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_item_id` (`batch_item_id`),
    UNIQUE KEY `uk_emp_month` (`employee_id`, `month`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工资条表';</pre>
  <h2 class="sec"><span class="sn">9</span>API 设计</h2>
  <div class="api">
    <div class="head">
      <span class="method">POST</span>
      <span class="path">/api/v1/salary/accounts</span>
      <span class="desc">创建薪资账套</span>
    </div>
    <div class="body">
      <h4>请求参数</h4>
      <div class="table-wrap">
        <table>
          <tr><th>参数</th><th>类型</th><th>必填</th><th>描述</th></tr>
          <tr><td>name</td><td>String</td><td class="req">是</td><td>账套名称</td></tr>
          <tr><td>scopeType</td><td>Integer</td><td class="req">是</td><td>1=部门 2=职位 3=职级</td></tr>
          <tr><td>scopeValue</td><td>String</td><td class="req">是</td><td>范围值</td></tr>
          <tr><td>effectiveDate</td><td>Date</td><td class="req">是</td><td>生效日期</td></tr>
          <tr><td>items</td><td>Array</td><td class="req">是</td><td>工资项目列表</td></tr>
        </table>
      </div>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">PUT</span>
      <span class="path">/api/v1/salary/employees/{employeeId}</span>
      <span class="desc">设置/更新员工薪资档案</span>
    </div>
    <div class="body">
      <h4>请求参数</h4>
      <div class="table-wrap">
        <table>
          <tr><th>参数</th><th>类型</th><th>必填</th><th>描述</th></tr>
          <tr><td>accountId</td><td>Long</td><td class="req">是</td><td>薪资账套ID</td></tr>
          <tr><td>baseSalary</td><td>BigDecimal</td><td class="req">是</td><td>基本工资</td></tr>
          <tr><td>allowanceBase</td><td>BigDecimal</td><td>否</td><td>津贴基数</td></tr>
          <tr><td>socialInsuranceBase</td><td>BigDecimal</td><td class="req">是</td><td>社保基数</td></tr>
          <tr><td>housingFundBase</td><td>BigDecimal</td><td class="req">是</td><td>公积金基数</td></tr>
          <tr><td>performanceBase</td><td>BigDecimal</td><td>否</td><td>绩效基数</td></tr>
          <tr><td>probationRatio</td><td>BigDecimal</td><td>否</td><td>试用期比例 0.8~1.0</td></tr>
          <tr><td>effectiveDate</td><td>Date</td><td class="req">是</td><td>生效日期</td></tr>
          <tr><td>adjustReason</td><td>String</td><td>否</td><td>调薪原因（变化时必填）</td></tr>
        </table>
      </div>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">POST</span>
      <span class="path">/api/v1/salary/batches</span>
      <span class="desc">创建薪资批次</span>
    </div>
    <div class="body">
      <h4>请求参数</h4>
      <div class="table-wrap">
        <table>
          <tr><th>参数</th><th>类型</th><th>必填</th><th>描述</th></tr>
          <tr><td>month</td><td>String</td><td class="req">是</td><td>薪资月份 yyyy-MM</td></tr>
          <tr><td>scopeType</td><td>Integer</td><td class="req">是</td><td>1=全公司 2=部门 3=个人</td></tr>
          <tr><td>scopeValue</td><td>String</td><td>否</td><td>范围值</td></tr>
        </table>
      </div>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">POST</span>
      <span class="path">/api/v1/salary/batches/{id}/calculate</span>
      <span class="desc">触发薪资核算</span>
    </div>
    <div class="body">
      <h4>响应示例</h4>
<pre>{
  "code": 0,
  "message": "success",
  "data": {
    "batchId": 2024,
    "batchNo": "SAL-202407-001",
    "status": "PENDING_CONFIRM",
    "totalCount": 156,
    "totalGross": 2856421.50,
    "totalNet": 2218436.78,
    "exceptionSummary": {
      "yellowCount": 8,
      "redCount": 2,
      "blockCount": 1
    }
  }
}</pre>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">POST</span>
      <span class="path">/api/v1/salary/batches/{id}/submit</span>
      <span class="desc">提交财务审批</span>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">GET</span>
      <span class="path">/api/v1/salary/batches/{id}/preview</span>
      <span class="desc">薪资预览（含异常标记）</span>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">GET</span>
      <span class="path">/api/v1/salary/batches/{id}/visualization</span>
      <span class="desc">薪资可视化数据（AntV）</span>
    </div>
    <div class="body">
      <h4>响应数据集</h4>
      <div class="table-wrap">
        <table>
          <tr><th>图表</th><th>图表类型</th><th>数据内容</th><th>适用角色</th></tr>
          <tr><td>薪资成本月度趋势</td><td>折线图</td><td>近 6 个月应发/实发总额变化</td><td>HR、财务</td></tr>
          <tr><td>部门薪资分布</td><td>柱状图</td><td>各部门薪资成本对比</td><td>HR、财务</td></tr>
          <tr><td>薪资构成占比</td><td>饼图/环形图</td><td>当月各项目占比</td><td>HR</td></tr>
          <tr><td>社保公积金对比</td><td>分组柱状图</td><td>各项社保公积金扣除对比</td><td>HR、财务</td></tr>
          <tr><td>薪资变动分布</td><td>直方图</td><td>薪资较上月变动幅度员工分布</td><td>HR、财务</td></tr>
        </table>
      </div>
    </div>
  </div>
  <div class="api">
    <div class="head">
      <span class="method">GET</span>
      <span class="path">/api/v1/salary/payslips/{month}</span>
      <span class="desc">员工查看工资条（需二次验证）</span>
    </div>
    <div class="body">
      <h4>请求头</h4>
      <div class="table-wrap">
        <table>
          <tr><th>Header</th><th>必填</th><th>描述</th></tr>
          <tr><td>X-Verify-Token</td><td class="req">是</td><td>二次验证 token（短信验证码换取）</td></tr>
        </table>
      </div>
      <h4>响应示例</h4>
<pre>{
  "code": 0,
  "message": "success",
  "data": {
    "month": "2024-07",
    "employeeNo": "202401005",
    "items": [
      { "name": "基本工资", "amount": 15000.00 },
      { "name": "岗位津贴", "amount": 3000.00 },
      { "name": "绩效奖金", "amount": 2000.00 },
      { "name": "迟到扣款", "amount": -100.00 },
      { "name": "社保扣除", "amount": -1575.00 },
      { "name": "公积金扣除", "amount": -1800.00 },
      { "name": "个人所得税", "amount": -412.50 }
    ],
    "grossSalary": 19800.00,
    "netSalary": 15912.50,
    "trend": [
      { "month": "2024-02", "netSalary": 15200.00 },
      { "month": "2024-03", "netSalary": 15500.00 },
      { "month": "2024-04", "netSalary": 15600.00 },
      { "month": "2024-05", "netSalary": 15800.00 },
      { "month": "2024-06", "netSalary": 16000.00 },
      { "month": "2024-07", "netSalary": 15912.50 }
    ]
  }
}</pre>
    </div>
  </div>
  <h2 class="sec"><span class="sn">10</span>关键技术设计</h2>
  <h3 class="sub">10-1 薪资计算引擎方案</h3>
  <p>采用规则引擎 + 公式表达式分离设计：</p>
  <ul class="bullets">
    <li>固定收入项：直接取 <code class="inline">employee_salary</code> 表对应字段值</li>
    <li>变动收入项：使用 Aviator 表达式引擎，公式示例：<code class="inline">performanceBase * performanceCoeff</code>、<code class="inline">hourlyRate * multiplier * overtimeHours</code></li>
    <li>考勤扣款项：从 <code class="inline">attendance_stat</code> 拉取数据按规则计算：<code class="inline">50 * lateCount</code>、<code class="inline">dailyWage * leaveDays</code></li>
    <li>社保公积金：基数 × 比例（养老 8% / 医疗 2% / 失业 0.5% / 公积金 12%）</li>
    <li>个税：累计预扣法，按年累计应纳税额查找对应税率区间</li>
    <li>试用期员工：基本工资与津贴按 <code class="inline">probationRatio</code> 折算，社保公积金按全额基数</li>
  </ul>
  <h3 class="sub">10-2 异常检测规则方案</h3>
<pre>规则 1：当月请假天数 &gt; 15 天         → 黄色预警
规则 2：当月加班费 &gt; 50 小时          → 黄色预警
规则 3：薪资较上月变动 &gt; 30%          → 红色预警（需 HR 确认）
规则 4：新入职员工未设置薪资档案       → 红色阻断（无法计算，跳过）
规则 5：实发工资 ≤ 0                  → 红色阻断（数据异常）
异常处理：
  - 黄色预警：标记但可继续提交审批
  - 红色预警：必须确认后才能提交
  - 红色阻断：跳过该员工，批次可继续</pre>
  <h3 class="sub">10-3 薪资数据安全方案</h3>
  <ul class="bullets">
    <li>薪资档案、明细表访问全程审计日志，记录操作人、时间、IP</li>
    <li>工资条查看需二次验证（短信验证码 / 密码），token 有效期 5 分钟，单次使用</li>
    <li>薪资列表导出需 HR 申请 + 财务审批，导出文件水印（操作人 + 时间）</li>
    <li>接口层基于角色控制：HR 看全量、财务看汇总、员工仅看本人</li>
    <li>数据库敏感字段（如 <code class="inline">base_salary</code>）可选 AES-256 加密存储</li>
  </ul>
  <h3 class="sub">10-4 个税累计预扣法计算</h3>
<pre>累计预扣预缴应纳税所得额 =
    累计收入 - 累计免税收入 - 累计减除费用 - 累计专项扣除 - 累计专项附加扣除
本期应预扣预缴税额 =
    (累计预扣预缴应纳税所得额 × 预扣率 - 速算扣除数) - 累计已预扣税额
预扣率表（综合所得）：
  不超过 36000 元            3%      0
  36000-144000 元           10%    2520
  144000-300000 元          20%   16920
  300000-420000 元          25%   31920
  420000-660000 元          30%   52920
  660000-960000 元          35%   85920
  超过 960000 元            45%  181920</pre>
  <h3 class="sub">10-5 数据可视化方案（AntV）</h3>
  <ul class="bullets">
    <li>折线图：<code class="inline">@antv/g2 Line</code>，薪资成本月度趋势（应发/实发双线）</li>
    <li>柱状图：<code class="inline">@antv/g2 Column</code>，部门薪资分布与社保公积金对比</li>
    <li>饼图：<code class="inline">@antv/g2 Pie</code>，薪资构成占比</li>
    <li>直方图：<code class="inline">@antv/g2 Histogram</code>，薪资变动分布</li>
    <li>个人趋势图：员工"我的薪资"页使用 <code class="inline">Line</code> 展示近 6 月实发工资</li>
    <li>异常看板：HR 预览页突出展示黄色/红色异常员工列表，支持快速跳转调整</li>
  </ul>
  <h2 class="sec"><span class="sn">11</span>排期</h2>
  <div class="schedule">
    <div class="schedule-row"><div class="idx">01</div><div class="stage">需求评审<small>评审产品规格，确认账套字段、异常规则、审批流</small></div><div class="dur">1 天</div></div>
    <div class="schedule-row"><div class="idx">02</div><div class="stage">技术方案<small>完成系分评审，确认计算引擎与数据库方案</small></div><div class="dur">2 天</div></div>
    <div class="schedule-row"><div class="idx">03</div><div class="stage">数据库开发<small>建表、索引、初始化账套与税率配置</small></div><div class="dur">2 天</div></div>
    <div class="schedule-row"><div class="idx">04</div><div class="stage">后端开发<small>账套、薪资档案、核算引擎、异常检测、审批</small></div><div class="dur">10 天</div></div>
    <div class="schedule-row"><div class="idx">05</div><div class="stage">前端开发<small>账套配置、核算预览、工资条、AntV 看板</small></div><div class="dur">9 天</div></div>
    <div class="schedule-row"><div class="idx">06</div><div class="stage">联调测试<small>前后端联调、核算准确性测试、异常场景测试</small></div><div class="dur">5 天</div></div>
    <div class="schedule-row"><div class="idx">07</div><div class="stage">回归上线<small>全量回归、预发验证、正式上线</small></div><div class="dur">2 天</div></div>
  </div>
  <div class="schedule-total">
    <span>总预估工期</span>
    <span class="v">31 个工作日</span>
  </div>
  <div class="footer-pg">
    <span>HRMS-ATT-SAL-SAD · v1.0</span>
    <span>END OF DOCUMENT</span>
  </div>
</section>
</div>
</body>
</html>
