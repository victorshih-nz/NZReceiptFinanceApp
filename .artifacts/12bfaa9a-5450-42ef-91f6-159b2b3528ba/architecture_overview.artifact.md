# NzReceiptApp 架構與資料流 (Architecture & Data Flow)

這份文件詳述了 **Scanner 掃描工作流** 的系統架構與組件間的互動流程。

## 掃描流程圖 (Scanning Workflow)

```mermaid
graph TD
    %% Central Hub: ViewModel
    VM[ScannerViewModel<br/>Core State & Flow Controller]

    %% Presentation Layer
    subgraph UI_Layer [UI / Presentation Layer]
        UI[ScannerFragment<br/>User Interface]
    end

    %% Data Layer - OCR
    subgraph Data_OCR [Data Layer - OCR Service]
        MLKit[MLKitOCRService<br/>Google ML Kit Engine]
    end

    %% Domain Layer
    subgraph Domain_Layer [Domain Layer - Business Logic]
        ParseUC[ParseReceiptUseCase]
        Parser[ParserProvider & WoolworthsParser]
        SaveUC[SaveReceiptUseCase]
    end

    %% Data Layer - Persistence
    subgraph Data_Storage [Data Layer - Persistence]
        Repo[ReceiptRepositoryImpl]
        DB[(SQLite / Room DB)]
    end

    %% Interactions & Data Flow
    UI -- 1. Capture/Select Image URI --> VM
    VM -- 2. Call processReceiptImage --> MLKit
    MLKit -- 3. Return rawText --> VM
    VM -- 4. Call ParseReceiptUseCase --> ParseUC
    ParseUC --> Parser
    Parser -- 5. Return Receipt Domain Object --> VM
    VM -- 6. Call SaveReceiptUseCase --> SaveUC
    SaveUC --> Repo
    Repo --> DB
    DB -- 7. Confirm Save Completion --> VM
    VM -- 8. Update LiveData state = SUCCESS --> UI
    UI -- 9. Show Toast / Stop Loading --> UI

    %% High-Contrast Styling
    classDef default fill:#1e1e1e,stroke:#cccccc,stroke-width:1px,color:#ffffff;
    classDef vmStyle fill:#3d2c00,stroke:#ffb300,stroke-width:2px,color:#ffffff;
    classDef uiStyle fill:#002b4d,stroke:#29b6f6,stroke-width:1.5px,color:#ffffff;
    classDef domainStyle fill:#0d3814,stroke:#66bb6a,stroke-width:1.5px,color:#ffffff;
    classDef dataStyle fill:#311b92,stroke:#ab47bc,stroke-width:1.5px,color:#ffffff;

    class VM vmStyle;
    class UI uiStyle;
    class ParseUC,Parser,SaveUC domainStyle;
    class MLKit,Repo,DB dataStyle;
```

---

### 流程說明 (Step-by-Step Explanation)

1.  **UI 觸發**：使用者在 `ScannerFragment` 透過拍攝或相簿選取圖片，傳遞 URI 給 ViewModel。
2.  **啟動 OCR**：`ScannerViewModel` 呼叫 `MLKitOCRService`。
3.  **文字回傳**：Google ML Kit 辨識完成後回傳 `rawText`。
4.  **執行解析**：ViewModel 將文字交給 `ParseReceiptUseCase`。
5.  **領域轉換**：`WoolworthsParser` (或其他) 將文字轉化為 `Receipt` 領域物件。
6.  **執行儲存**：ViewModel 呼叫 `SaveReceiptUseCase` 啟動持久化流程。
7.  **寫入資料庫**：`ReceiptRepositoryImpl` 將領域物件轉為 Room Entity 並由 `ReceiptDao` 寫入 SQLite。
8.  **狀態更新**：成功後 ViewModel 更新 `state` LiveData。
9.  **UI 反饋**：Fragment 觀察到成功狀態，停止載入動畫並提示使用者。
