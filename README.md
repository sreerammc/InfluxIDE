# InfluxDB Query IDE

A modern, user-friendly JavaFX-based IDE for querying InfluxDB databases with features like drag-and-drop, filtering, sorting, and CSV export.

## 🚀 Features

- **Modern JavaFX UI** - Clean, intuitive interface
- **Database Connection** - Easy setup for InfluxDB connections
- **Query Editor** - SQL query input with syntax highlighting
- **Results Display** - Tabular results with Excel-like filtering and sorting
- **Drag & Drop** - Drag table names from results to query editor
- **CSV Export** - Export query results to CSV format
- **Database Explorer** - Show tables, measurements, and databases
- **Standalone Package** - Self-contained distribution with JavaFX runtime

## 🛠️ Requirements

- **Java 11 or higher**
- **Maven 3.6+** (for development)
- **InfluxDB 3.x** database

## 📦 Installation

### Option 1: Standalone Package (Recommended for Users)
1. Download the `InfluxDB-IDE-Standalone` package
2. Extract to any folder
3. Double-click `InfluxDB-IDE.bat` to run

### Option 2: Build from Source (For Developers)
```bash
# Clone the repository
git clone <your-repo-url>
cd influxSimple

# Build the project
mvn clean package

# Run with Maven
mvn javafx:run
```

## 🔧 Configuration

### Connection Settings
- **Host**: Your InfluxDB server address (e.g., `172.187.233.15`)
- **Port**: InfluxDB port (e.g., `8181`)
- **Database**: Database name (e.g., `test`)
- **API Key**: Your InfluxDB API token

**⚠️ Authentication Note:** This application only supports **API Key (Token) authentication**. It does **NOT** support OAuth, SAML, or other authentication methods. You must use a valid InfluxDB API token for authentication.

### Example Connection
```
Host: 172.187.233.15
Port: 8181
Database: test
API Key: apiv3_your_token_here
```

## 📖 Usage

### Basic Query
1. Enter your connection details
2. Click "Connect" to establish connection
3. Type your SQL query (e.g., `SELECT * FROM sensor_f`)
4. Click "Execute Query" to run
5. View results in the table format

### Database Exploration
- **Show Tables**: Lists all measurements in the database
- **Export to CSV**: Save results to CSV file
- **Filter & Sort**: Use Excel-like features on results

### Advanced Features
- **Drag & Drop**: Drag table names from results to query editor
- **Global Filter**: Filter all columns at once
- **Column Filters**: Individual column filtering
- **Column Sorting**: Click column headers to sort

## 🏗️ Project Structure

```
influxSimple/
├── src/main/java/com/influxdata/demo/
│   └── InfluxDBJavaFXIDE.java    # Main application
├── src/main/resources/
│   └── icons/                     # Application icons
├── pom.xml                        # Maven configuration
├── InfluxDB-IDE.bat              # Windows launcher
├── run.sh                         # Unix/Linux launcher
├── create-distribution.ps1        # PowerShell distribution creator
├── create-distribution.bat        # Batch distribution creator
└── README.md                      # This file
```

## 🔨 Development

### Building
```bash
mvn clean package
```

### Running Tests
```bash
mvn test
```

### Creating Standalone Package
```bash
# Build the project first
mvn clean package

# Create distribution package (Windows)
.\create-distribution.ps1
# OR
create-distribution.bat

# This creates a complete standalone ZIP package
# that includes JavaFX runtime and launcher scripts
```

## 📝 Dependencies

- **JavaFX 24.0.2** - UI framework
- **InfluxDB Java Client** - Database connectivity
- **JSON Library** - Data parsing
- **Maven** - Build tool

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Sreeram C Machavaram**

## 🐛 Troubleshooting

### Common Issues

#### JavaFX Runtime Missing
- Ensure Java 11+ is installed
- Use the standalone package which includes JavaFX
- Check that `javafx-sdk` folder is present

#### Connection Issues
- Verify InfluxDB server is running
- Check host, port, and API key
- Ensure database exists and is accessible
- **Authentication Method**: This app only supports API Key authentication, not OAuth/SAML

#### Build Issues
- Clean and rebuild: `mvn clean package`
- Check Java version: `java -version`
- Verify Maven installation: `mvn -version`

## 📞 Support

For issues and questions:
1. Check the troubleshooting section above
2. Search existing issues in the repository
3. Create a new issue with detailed information

---

**Happy Querying! 🚀** 