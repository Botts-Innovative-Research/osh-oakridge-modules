/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.system.database;


import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.database.IObsSystemDbAutoPurgePolicy;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.slf4j.Logger;
import org.vast.data.DataBlockTuple;
import org.vast.util.TimeExtent;

import java.time.Instant;
import java.util.*;

/**
 * Implementation of a purging policy for removing video records that are not during valid alarming occupancies
 *
 * @author Kalyn Stricklin
 * @since May 2025
 */
public class OccupancyVideoPurgePolicy implements IObsSystemDbAutoPurgePolicy {

    OccupancyVideoPurgePolicyConfig config;

    OccupancyVideoPurgePolicy(OccupancyVideoPurgePolicyConfig config) {
        this.config = config;
    }


   // purge video records from the database unless it coincides with a valid alarming occupancy

    @Override
    public void trimStorage(IObsSystemDatabase db, Logger log, Collection<String> systemUIDs) {
        // remove all video frames saved except during an alarming occupancy

       List<TimeExtent> timeIntervalsToKeep = new ArrayList<>();

        long numObsRemoved = 0;

        // don't use a systemUID filter yet bc we need to access the rpms
        var dataStreams = db.getDataStreamStore().selectEntries(new DataStreamFilter.Builder().build()).iterator();

        while (dataStreams.hasNext())
        {
            var dsEntry = dataStreams.next();
            var dsID = dsEntry.getKey().getInternalID();
            var recordStructure = dsEntry.getValue().getRecordStructure();
            var resultTimeRange = dsEntry.getValue().getResultTimeRange();


            //todo: identify occupancy datastreams
            if(recordStructure.getDefinition().equals("http://www.opengis.net/def/occupancy")){

                //todo: get observations from datastream
                var obsStore = db.getObservationStore().selectEntries(new ObsFilter.Builder()
                        .withDataStreams(dsID)
                        .build()).iterator();

                //todo: extract data from observations to
              while(obsStore.hasNext()){
                  var obsEntry = obsStore.next();

                  var result = obsEntry.getValue().getResult();

                  if(result instanceof DataBlockTuple){
                     var startTime = result.getDateTime(2);
                     var endTime = result.getDateTime(3);
                     var gammaAlarm = result.getBooleanValue(5);
                     var neutronAlarm = result.getBooleanValue(6);

                      //todo: add to list of video intervals to keep based of alarming occupancy start and end times
                      if(gammaAlarm || neutronAlarm){
                          timeIntervalsToKeep.add(TimeExtent.period(startTime.toInstant(), endTime.toInstant()));

                      }
                  }
              }
            }

            //todo: remove video records outside of videoIntervalsToKeep
            // if videoDS and timeIntervalsToKeep then remove db
             if(recordStructure.getDefinition().equals("http://sensorml.com/ont/swe/property/VideoFrame") && timeIntervalsToKeep.size() > 0){

                 System.out.println("video record and time range found");

                 System.out.println("time ints to keep: "+ timeIntervalsToKeep);

                 List<TimeExtent> mergedTimeIntervalsToKeep = new ArrayList<>();

                 int idx = 0;
                 while(idx < timeIntervalsToKeep.size()){
                     Instant start = timeIntervalsToKeep.get(idx).begin();
                     Instant end = timeIntervalsToKeep.get(idx).end();

                     // check that index is still in range and if end time equals next intervals start time then update the end time
                     while(idx+1 < timeIntervalsToKeep.size() && end.equals(timeIntervalsToKeep.get(idx+1).begin())){
                         System.out.println("end time matches next intervals start time... merging times");
                         //update the end time
                         end = timeIntervalsToKeep.get(idx+1).end();
                         idx++;
                     }

                     mergedTimeIntervalsToKeep.add(TimeExtent.period(start, end));
                     idx++;
                 }


                 System.out.println("new merged time intervals: "+ mergedTimeIntervalsToKeep);

                 // check if the resultTimeRange is before the first occupancy start time if so call this!!!
                 if(resultTimeRange.begin().isBefore(mergedTimeIntervalsToKeep.get(0).begin())){
                     // then remove records from the video result time start til our first alarming occupancy
                     System.out.println("Removing records before first alarm: " + resultTimeRange.begin() + " to " + mergedTimeIntervalsToKeep.get(0).begin());
                     numObsRemoved += db.getObservationStore().removeEntries(new ObsFilter.Builder()
                             .withDataStreams(dsID)
                             .withResultTimeDuring(resultTimeRange.begin(), mergedTimeIntervalsToKeep.get(0).begin())
                             .build());
                 }

                 // now purge anything after the last item in the interval and the end of the result time of the video
                 var lastIndex = mergedTimeIntervalsToKeep.size()-1;

                 if(resultTimeRange.end().isAfter(mergedTimeIntervalsToKeep.get(lastIndex).end())){
                     System.out.println("Removing records after last alarm: " + mergedTimeIntervalsToKeep.get(lastIndex).end()  + " to "  + resultTimeRange.end());

                     Instant start = mergedTimeIntervalsToKeep.get(lastIndex).end();
                     Instant end = resultTimeRange.end().minusSeconds(10);
                     System.out.println("Attempting to purge from: " + start + " to " + end);
                     // then remove records from the video result time start til our first alarming occupancy
                     numObsRemoved += db.getObservationStore().removeEntries(new ObsFilter.Builder()
                             .withDataStreams(dsID)
                             .withResultTimeDuring(start, end)
                             .build());
                 }

                 // now remove entries between alarming occupancies
                 int i = 0;
                 while(i < mergedTimeIntervalsToKeep.size()-1){

                     var currentIntervalEndTime = mergedTimeIntervalsToKeep.get(i).end();
                     var nextIntervalStartTime = mergedTimeIntervalsToKeep.get(i+1).begin();

                     System.out.println("time interval end: "+ mergedTimeIntervalsToKeep.get(i).end());
                     System.out.println("next indexes time interval start: "+ mergedTimeIntervalsToKeep.get(i+1).begin());

                     if(currentIntervalEndTime.isBefore(nextIntervalStartTime)){
                         numObsRemoved += db.getObservationStore().removeEntries(new ObsFilter.Builder()
                                 .withDataStreams(dsID)
                                 .withResultTimeDuring(currentIntervalEndTime, nextIntervalStartTime)
                                 .build());
                         System.out.println("removed observation: "+ currentIntervalEndTime +"-" + nextIntervalStartTime);
                     }
                     i++;
                 }


             }




        }
        if (log.isInfoEnabled()) {

            log.info("Purgin Data. Removed records: {}", numObsRemoved);
        }
    }
}

