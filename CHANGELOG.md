# Positioning SDK Change Log

### Version 2.3.1 (2025-02-10)
#### Bug Fix🐞:
* Fixed an issue where the buildingId and type properties of mapxusLocation were incorrectly set when resume positioning within a very short period.

### Version 2.3.0 (2025-01-14)
#### Features🎉:
* Compatibility with shared floor data:
  - `MapxusLocation` now includes `venueId`
  - `MapxusFloor` now includes `type`.
  - Add `FloorType` enum with `FLOOR` and `SHARED_FLOOR` values.
* Enhancement of direction sensor.

#### Bug Fix🐞:
* Fix the bug in getOrdinal of `MapxusFloor`.
* Fixed issues in `onError` callback.
  - `ERROR_SERVER_EXCEPTION` (Error code: 107): Triggered when authentication fails.
  - `WARNING` (Error code: 108): Triggered when indoor positioning fails (i.e., no matching indoor location found).


### Version 2.2.9 (2024-12-11)
#### Bug Fix🐞:
* Fixed an NPE issue when start positioning in an abnormal state, such as network disconnection, location not open, etc.

### Version 2.2.8 (2024-11-26)
#### Features🎉:
* Added debugging mode support.

#### Breaking 💥:
* Update the user authentication system.
* Change minSdkVersion from 21 and above(minSdkVersion >= 21) to 24 and above(minSdkVersion >= 24).

***Note***：After the system update, some app ids may no longer be available, please contact us <support@mapxus.com> in time to update your app id.


### Version 2.2.7 (2024-10-24)
#### Bug Fix🐞:
* Fix issues caused by Android Background Execution Limits.

### Version 2.2.6 (2023-09-26)
#### Features🎉:
* Added error info callback in onErrorChange event when the sensor is missing.

### Version 2.2.5 (2023-09-10)
#### Bug Fix🐞:
* Fixed an NPE when service failed to stop properly.

### Version 2.2.4 (2023-05-17)
#### Bug Fix🐞:
* Fixed an issue where the location deviates significantly from the anomaly.

### Version 2.2.3 (2023-03-24)
#### Features🎉:
* Update GMS services version from 17.1.0 to 21.0.1.

#### Bug Fix🐞:
* Add "HIGH_SAMPLING_RATE_SENSORS" permission to support Android 12 and above.

### Version 2.2.2 (2022-10-26)
#### Bug Fix🐞:
* Prevent listeners will be registered repeatedly in the same application when use multiple activities.

### Version 2.2.1 (2022-09-07)
#### Bug Fix🐞:
* Fix sensor data collection calculations.

### Version 2.2.0 (2022-03-04)
#### Features🎉:
* Optimize the performance of indoor and outdoor switching.

### Version 2.1.1 (2021-11-10)
#### Features🎉:
* Update dependencies version.

### Version 2.1.0 (2021-11-03)
#### Bug Fix🐞:
* Fix Wifi filter logic bug in positioning sdk.

### Version 2.0.9 (2021-06-02)
#### Features🎉:
* Optimize the performance of Indoor/Outdoor Detection
* Change SDK download repository from jcenter(Deprecated) to MavenCentral

### Version 2.0.8 (2021-03-10)
#### Bug Fix🐞:
* Fix Wi-Fi scanning problem

### Version 2.0.7 (2021-01-11)
#### Bug Fix🐞:
* Fix the problem of slow positioning response time when users are outdoors
* Fix app crashes accidentally when network quality is poor

### Version 2.0.6 (2020-11-24)
#### Bug Fix🐞:
* Fix android resources' names conflict

### Version 2.0.5 (2020-11-02)
#### Features🎉:
* Optimize compass accuracy

### Version 2.0.4 (2020-10-12)
#### Bug Fix🐞:
* Fix PDR missing problem


### Version 2.0.3 (2020-09-18)
#### Bug Fix🐞:
* Fix abnormal compass reading problem
* Fix some caching data problems

### Version 2.0.2 (2020-06-24)

#### Bug Fix🐞:
* Fixed abnormal positioning accuracy problem when start positioning or switching indoor/outdoor.


### Version 2.0.1 (2020-06-23)

#### Bug Fix🐞:
* Fixed cannot switch between indoor and outdoor problem.

### Version 2.0.0 (2020-06-17)

#### Features🎉:
* Support Android 9+
* Much faster TTFF(Time to first fix)
* Faster Floor Detection Response
* Lifecycle Aware
* Add java8 support
* Aar size reduced about 40%, 250KB
* Cut unnecessary permissions
* Remove background positioning feature for user privacy concern

#### Bug Fix🐞:
* Fixed the strange location bouncing problem

### Version 1.0.0 (2020-01-16)

* Change backend service url

### Version 0.4.0

* Change authorization sever

### Version 0.3.14

* Modify the interface that returns the orientation accuracy level

### Version 0.3.13

* Return the orientation accuracy level when the orientation returns. You can tilt and move your phone like a 8-shape when the accuracy is not high
* Add and modify some error message to return

### Version 0.3.12

* Fix the bug of which some phone models do not take new algorithm

### Version 0.3.11

* Change positioning algorithms

### Version 0.3.10

* Fix bugs in positioning algorithms
* Multiple attempts if network error happens during positioning

### Version 0.3.9

* Correct switching problem between indoor positioning and outdoor positioning and fix issue of low-speed, inaccurate floor change
* Fix multi-threaded security vulnerability and optimize positioning process


### Version 0.3.8

* Change naming of accuracy return interface


### Version 0.3.7

* Add positioning accuracy return in positioning information, unit: metre
* Shorten initial positioning time
* Fix multi-threaded security vulnerability and optimize positioning process
* Correct wrong return. Error of `ERROR_LOCATION_SERVICE_DISABLED` will be reported if the user does not activate location service or the location service is in high accuracy
  Notice: For using Mapxus Positioning map (version 0.3.7 or above), if you need to use Mapxus Map as well ,please update Mapxus Map to versioning **2.4.1** or above.

### Version 0.3.6

* Update new license package. The previous api id and secret will be invalid soon. Please contact us to acquire new id and secret;
* Optimize positioning algorithm;
* Positioning can only be realized in a single floor without a pressure sensor. `changeFloor` is needed for switching floors.

Notice: For using Mapxus Positioning map (version 0.3.6 or above), if you need to use Mapxus Map as well ,please update Mapxus Map to versioning **2.3.3-beta** or above.

### Version 0.3.5

* Support indoor positioning service for the device without pressure sensor

### Version 0.3.4

* Remove `Manifest.permission.WRITE_EXTERNAL_STORAGE` permission

### Version 0.3.3

* Remove `Manifest.permission.READ_PHONE_STATE` permission
* Use UUID

### Version 0.3.2

* Add utility class to check if the device supports GNSS or not

### version 0.3.1

* Continue to position if network disconnect during positioning (instead of stop positioning)

### Version 0.3.0

* Add outdoor positioning service (only for devices with GNSS). For outdoor positioning, the returned information is null building and floor.
* Add `WARNING`, which will notice the user but will not exit positioning service
* Implement change floor interface during positioning
* Change name and package name address of each interface
* Change to release package repository which can be automated integrated by jcenter
* Resolve pom file deficiency of upload package