# GrayTrack — grayscale live camera object tracking

Android project for Android 9 (API 28) and newer. It shows a grayscale live camera feed, detects up to five ML Kit objects, draws green boxes, and smooths box movement.

## Important behavior and limitations
- ML Kit's on-device object detector is a general object detector, not a universal detector for every possible object.
- The app ranks detections returned by ML Kit by bounding-box area and displays at most five. It cannot rank objects that the detector did not detect.
- Tracking IDs are supplied by ML Kit in stream mode. IDs can change after occlusion or scene changes.
- The grayscale image is used for display. ML Kit analyzes the camera's original YUV frame, which is generally more reliable than forcing a detector trained for color imagery to use grayscale input.
- Camera preview frames are reduced to a modest analysis resolution to balance speed and accuracy. Performance depends on the exact Note 9 variant, lighting, and scene.

## Build in Android Studio
1. Download and unzip this repository.
2. Open the `GrayTrackAndroid` folder in Android Studio.
3. Allow Gradle sync and install the Android SDK platform requested by Android Studio if needed.
4. Connect the Galaxy Note 9, enable Developer options and USB debugging, then press Run.
5. Accept the camera permission.

The project uses Gradle 8.9 / Android Gradle Plugin 8.7.3 and requires JDK 17.

## Build on GitHub
Push these files to a GitHub repository. The workflow `.github/workflows/android.yml` builds a debug APK and uploads it as a workflow artifact.
Open **Actions → Android CI → latest run → Artifacts → GrayTrack-debug-apk** to download it.

## Main paths
- `app/src/main/java/com/example/graytrack/MainActivity.kt` — app and camera lifecycle
- `app/src/main/java/com/example/graytrack/camera/CameraController.kt` — CameraX setup and analysis
- `app/src/main/java/com/example/graytrack/analyzer/ObjectAnalyzer.kt` — ML Kit object detection
- `app/src/main/java/com/example/graytrack/tracking/ObjectTracker.kt` — box smoothing
- `app/src/main/java/com/example/graytrack/overlay/GrayscaleOverlayView.kt` — grayscale display and green boxes

## Offline use
The detector is bundled through the ML Kit dependency, so after the APK is built and installed, analysis does not require internet access. Initial Gradle build/sync does require internet access to download dependencies.
