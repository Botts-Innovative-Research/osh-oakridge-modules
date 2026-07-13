# OSCAR Lane System Change Log
All notable changes to this project will be documented in this file. 

## [2.1.0] - 2026-07-13
### Added
- New "Mobile Detector System" module type (com.botts.impl.system.lane.mobile) for RS-350/D5 mobile detectors: single Detector Connection config, no fixed-lane options
- Lane-level RPM Remote Host/Port is now authoritative: pushed down to the RPM driver's comm settings on every lane (re)initialization
### Changed
- Occupancy process submodules are named "<lane> - Occupancy", created once and reused across restarts instead of being deleted and recreated
- Removed occupancy process members are cleaned up so they no longer resurrect themselves from leftover event-bus subscriptions

## [2.1.0] - 2025-10-29
### Changed
- Updated the name of occupancyId to be occupancyObsId to be clearer on which field is being used. 


## [2.0.1] - 2025-10-15
### Changed
- Fixed adjudication control when updating the occupancy observation output, updated tests to include check
- Removed Adjudication Enum and updated adjudication data record to use codes 0-11 for adjudicationCode
- Updated the data arrays in the adjudication record/ occupancy output
- 
## [2.0.0] - 2025-10-14
### Removed
- Removed the System Driver Database creation from lane system
- Removed the Occupancy Process creation from lane system

### Changed
- Updated LaneTests to include adjudication tests
### Added
- Added Adjudication control to Lane System
