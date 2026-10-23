# Aspect Change Log
All notable changes to this project will be documented in this file. 

## [Unreleased]
### Changed
- Occupancy start times and transition daily-file samples now share one millisecond timestamp so
  lane camera recordings correlate to the exact Aspect occupancy even between throttled samples.

## [2.0.0] - 2025-10-14
### Changed
- Removed adjudication control from driver
- Moved occupancy output to RadHelper for reusability

