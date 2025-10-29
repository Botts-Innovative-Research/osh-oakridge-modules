# OSCAR Lane System Change Log
All notable changes to this project will be documented in this file. 

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
