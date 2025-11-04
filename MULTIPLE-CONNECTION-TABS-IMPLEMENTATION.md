# Multiple Connection Tabs - Implementation Plan

## Complexity Assessment: **MODERATE** (4-6 hours)

### Current Architecture Challenges
1. ✅ **Modular UI Components** - QueryPanel and ResultsPanel are already self-contained (GOOD)
2. ⚠️ **Single State Management** - All state is in instance variables (NEEDS CHANGE)
3. ⚠️ **Service Layer** - Single InfluxDBService instance (NEEDS CHANGE)
4. ⚠️ **Event Handlers** - All tied to single panels (NEEDS CHANGE)

## Required Changes

### 1. Data Structures (NEW)
```java
// Connection Tab Data Container
public class ConnectionTab {
    private String tabId;
    private ApplicationConfig config;
    private InfluxDBService influxDBService;
    private QueryPanel queryPanel;
    private ResultsPanel resultsPanel;
    private List<Map<String, Object>> currentResults;
    private AtomicBoolean isQueryRunning;
    private Tab tab;
    
    // Constructor, getters, setters
}

// In MainApplicationController
private TabPane connectionTabPane;
private Map<String, ConnectionTab> connectionTabs = new HashMap<>();
private ConnectionTab activeTab;
```

### 2. UI Restructuring (MODERATE)
- Replace `querySection` and `resultsSection` with a `TabPane`
- Each `Tab` contains:
  - VBox with QueryPanel and ResultsPanel
  - Tab label: "Host:DB" or "Connection 1"
  - Close button on tab

### 3. Service Management (MODERATE)
- Create `InfluxDBService` per tab
- Track services in `ConnectionTab` objects
- Update menu handlers to use `activeTab.getInfluxDBService()`

### 4. Event Handling (MODERATE)
- Tab selection listener → update `activeTab`
- Update status bar when tab changes
- Route all menu actions to `activeTab`
- Handle tab close → cleanup resources

### 5. Thread Safety (LOW-MODERATE)
- Already using `AtomicBoolean` for query state
- Each tab has its own query state
- No shared mutable state between tabs

## Implementation Steps

### Step 1: Create ConnectionTab Class (30 min)
```java
// New file: src/main/java/com/influxdata/demo/model/ConnectionTab.java
public class ConnectionTab {
    private final String tabId;
    private ApplicationConfig config;
    private InfluxDBService influxDBService;
    private QueryPanel queryPanel;
    private ResultsPanel resultsPanel;
    private List<Map<String, Object>> currentResults;
    private final AtomicBoolean isQueryRunning = new AtomicBoolean(false);
    private Tab tab;
    
    // Constructor
    public ConnectionTab(String tabId, ApplicationConfig config) {
        this.tabId = tabId;
        this.config = config;
        this.influxDBService = new InfluxDBService(config);
        this.queryPanel = new QueryPanel();
        this.resultsPanel = new ResultsPanel();
        this.currentResults = new ArrayList<>();
    }
    
    // Cleanup method
    public void cleanup() {
        // Close connections if needed
        currentResults = null;
        queryPanel = null;
        resultsPanel = null;
    }
    
    // Getters and setters...
}
```

### Step 2: Refactor MainApplicationController (2-3 hours)
**Changes needed:**
1. Replace single panels with TabPane
2. Convert instance variables to per-tab storage
3. Update all handlers to use activeTab
4. Add tab creation/management methods

**Key methods to add:**
```java
private void createConnectionTab(ApplicationConfig config)
private void closeConnectionTab(String tabId)
private void switchToTab(ConnectionTab tab)
private ConnectionTab getActiveTab()
```

### Step 3: Update Menu Handlers (1 hour)
- All menu actions check `getActiveTab()` first
- Show error if no active tab
- Route to appropriate tab's service/panels

### Step 4: Update Status Bar (30 min)
- Show connection info for active tab
- Update on tab switch

### Step 5: Add "New Connection Tab" Menu Item (30 min)
- Database menu → "New Connection Tab"
- Opens connection dialog
- Creates new tab on success

## Code Structure Changes

### Before:
```java
private QueryPanel queryPanel;
private ResultsPanel resultsPanel;
private InfluxDBService influxDBService;
private ApplicationConfig currentConfig;

private void handleExecuteQuery() {
    // Uses queryPanel, influxDBService directly
}
```

### After:
```java
private TabPane connectionTabPane;
private Map<String, ConnectionTab> connectionTabs;
private ConnectionTab activeTab;

private void handleExecuteQuery() {
    ConnectionTab tab = getActiveTab();
    if (tab == null) return;
    
    // Use tab.getQueryPanel(), tab.getInfluxDBService()
}
```

## UI Layout Changes

### Before:
```
[Menu Bar]
[Header]
[Query Panel]
[Results Panel]
[Status Bar]
```

### After:
```
[Menu Bar]
[Header]
[TabPane]
  ├─ Tab 1: "host1:db1" [X]
  │   ├─ Query Panel (tab 1)
  │   └─ Results Panel (tab 1)
  ├─ Tab 2: "host2:db2" [X]
  │   ├─ Query Panel (tab 2)
  │   └─ Results Panel (tab 2)
  └─ [+ New Tab]
[Status Bar]
```

## Potential Challenges

1. **Memory**: Multiple result sets - need to monitor memory usage
2. **Tab Labels**: Auto-update when connection changes (easy with binding)
3. **Initial Tab**: First connection creates first tab automatically
4. **Tab Closing**: Prevent closing last tab? Or allow closing all?

## Testing Checklist

- [ ] Create multiple tabs with different connections
- [ ] Switch between tabs
- [ ] Execute queries in different tabs simultaneously
- [ ] Close tabs
- [ ] Menu actions work on active tab
- [ ] Status bar updates on tab switch
- [ ] Memory doesn't leak when closing tabs

## Estimated Timeline

- **Basic Implementation**: 4-6 hours
- **With Polish**: 6-8 hours
- **With Testing**: 8-10 hours

## Decision Needed

**Question**: Should we allow closing all tabs, or always keep at least one tab open?

**Recommendation**: Allow closing all tabs, but show a "Connect" button when no tabs exist.

