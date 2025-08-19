# Application Icons

This directory is for storing custom application icons.

## How to Add a Custom Icon

1. **Place your icon file here** with the name `app_icon.png`
2. **Recommended format**: PNG format, 32x32 or 64x64 pixels
3. **File name must be**: `app_icon.png` (exact match)

## Icon Requirements

- **Format**: PNG (recommended) or other JavaFX supported formats
- **Size**: 32x32, 64x64, or 128x128 pixels work best
- **Transparency**: Supported (PNG with alpha channel)
- **File size**: Keep under 100KB for best performance

## Fallback Behavior

If no custom icon is found, the application will automatically create a simple programmatic icon with:
- Blue rounded rectangle background
- White "IDB" text (InfluxDB)
- 32x32 pixel size

## Example Icon

You can create a simple icon using any image editor:
- Use InfluxDB brand colors (#2196F3 blue)
- Include database/time-series elements
- Keep it simple and recognizable at small sizes

## Troubleshooting

- **Icon not showing**: Check file name is exactly `app_icon.png`
- **Wrong size**: Ensure icon is square and reasonable dimensions
- **Format issues**: Convert to PNG if using other formats 