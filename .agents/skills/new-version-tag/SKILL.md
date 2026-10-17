---
name: new-version-tag
description: >-
  Draft Google Play release “What’s new” / update changelog text in the exact
  multi-locale tagged format Play Console expects. Requires a version range
  such as 1.1.0-1.2.0. Use when the user says 更新日志, 帮我写更新日志,
  GP 更新日志, Play what’s new, release notes, or /new-version-tag.
---

# Google Play 更新日志

根据版本改动，写出 Google Play「该版本的新内容 / What’s new」文案。

## 规则

1. **必须先拿到版本区间**：格式为 `旧版本-新版本`，例如 `1.1.0-1.2.0`。用户未给出时先问清，不要开始写日志。
2. 弄清该版本区间内的改动（用户说明、git log、版本号均可）。信息不够再问，不要空写。
3. **必须保留每种语言的开闭尖括号标签**，例如 `<en-US>` 与 `</en-US>`。只把更新日志正文填进对应语言的尖括号中间；禁止省略、转义、改写或删除任何一对尖括号标签。
4. 整段结果放进 **一个** 纯文本代码块（\`\`\`text ... \`\`\`）里输出，避免聊天界面把尖括号当 HTML 吃掉；代码块外不要再写标题或说明。用户复制时复制代码块内全部内容（含尖括号标签）粘贴到 Play Console。
5. **标签名与顺序必须与模板完全一致**，Google 才能识别。不要改成别的 locale、不要缺语言、不要调换顺序。
6. 每种语言 2–5 条要点，每条一行，以 `• `（项目符号 + 空格）开头。
7. 文案按各语言自然表达，不要整段机翻硬套；专有名词（如 Google Play、RAM、Wi‑Fi）可保留常见写法。
8. 不要写内部实现细节（类名、PR、厂商 ROM bug 排查过程）；面向普通用户。

## 输出模板（格式禁止改动）

下方尖括号标签必须原样出现在最终输出里；只替换标签内部的 `•` 条目内容。

```text
<en-US>
• First release on Google Play
• Test phone hardware and system features in one app
• Auto test, manual checks, device info, RAM status, and app uninstall
</en-US>
<hi-IN>
• Google Play पर पहली रिलीज़
• एक ऐप में फ़ोन हार्डवेयर और सिस्टम सुविधाओं की जाँच करें
• ऑटो टेस्ट, मैन्युअल जाँच, डिवाइस जानकारी, RAM स्थिति और ऐप अनइंस्टॉल
</hi-IN>
<ja-JP>
• Google Play に初公開
• スマホのハードウェアとシステム機能を1つのアプリでテスト
• 自動テスト、手動テスト、端末情報、RAM 状態、アプリのアンインストールに対応
</ja-JP>
<ko-KR>
• Google Play 첫 출시
• 한 앱에서 휴대폰 하드웨어와 시스템 기능을 테스트하세요
• 자동 테스트, 수동 테스트, 기기 정보, RAM 상태, 앱 삭제 지원
</ko-KR>
<zh-TW>
• 應用程式首次上架 Google Play
• 一站式檢測手機軟硬體是否正常
• 支援自動測試、手動測試、裝置資訊、記憶體狀態與解除安裝應用程式
</zh-TW>
<th>
• เปิดตัวครั้งแรกบน Google Play
• ทดสอบฮาร์ดแวร์และฟีเจอร์ระบบของมือถือในแอปเดียว
• ทดสอบอัตโนมัติ ทดสอบด้วยตนเอง ข้อมูลอุปกรณ์ สถานะ RAM และถอนการติดตั้งแอป
</th>
<vi>
• Phát hành lần đầu trên Google Play
• Kiểm tra phần cứng và tính năng hệ thống của điện thoại trong một ứng dụng
• Hỗ trợ kiểm tra tự động, kiểm tra thủ công, thông tin thiết bị, trạng thái RAM và gỡ cài đặt ứng dụng
</vi>
<zh-CN>
• 应用首次上架 Google Play
• 一站式检测手机软硬件是否正常
• 支持自动测试、手动测试、设备信息、内存状态与卸载应用
</zh-CN>
<it-IT>
• Prima pubblicazione su Google Play
• Testa hardware e funzioni di sistema del telefono in un’unica app
• Test automatici, test manuali, info dispositivo, stato della RAM e disinstallazione app
</it-IT>
```

语言与标签顺序固定为：`en-US` → `hi-IN` → `ja-JP` → `ko-KR` → `zh-TW` → `th` → `vi` → `zh-CN` → `it-IT`。
