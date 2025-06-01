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


            //identify occupancy datastreams
            if(recordStructure.getDefinition().equals("http://www.opengis.net/def/occupancy")){

                //get observations from datastream
                var obsStore = db.getObservationStore().selectEntries(new ObsFilter.Builder()
                        .withDataStreams(dsID)
                        .build()).iterator();

                //extract data from observations to
              while(obsStore.hasNext()){
                  var obsEntry = obsStore.next();

                  var result = obsEntry.getValue().getResult();

                  if(result instanceof DataBlockTuple){
                     var startTime = result.getDateTime(2);
                     var endTime = result.getDateTime(3);
                     var gammaAlarm = result.getBooleanValue(5);
                     var neutronAlarm = result.getBooleanValue(6);

                      // add to list of video intervals to keep based of alarming occupancy start and end times
                      if(gammaAlarm || neutronAlarm){
                          timeIntervalsToKeep.add(TimeExtent.period(startTime.toInstant(), endTime.toInstant()));

                      }
                  }
              }
            }

            // remove video records outside of videoIntervalsToKeep
            // if videoDS and timeIntervalsToKeep then remove db
             if(recordStructure.getDefinition().equals("http://sensorml.com/ont/swe/property/VideoFrame") && timeIntervalsToKeep.size() > 0){

                 List<TimeExtent> mergedTimeIntervalsToKeep = new ArrayList<>();

                 int idx = 0;
                 while(idx < timeIntervalsToKeep.size()){
                     Instant start = timeIntervalsToKeep.get(idx).begin();
                     Instant end = timeIntervalsToKeep.get(idx).end();

                     // check that index is still in range and if end time equals next intervals start time then update the end time
                     while(idx+1 < timeIntervalsToKeep.size() && end.equals(timeIntervalsToKeep.get(idx+1).begin())){
                         //update the end time
                         end = timeIntervalsToKeep.get(idx+1).end();
                         idx++;
                     }

                     mergedTimeIntervalsToKeep.add(TimeExtent.period(start, end));
                     idx++;
                 }


                 // check if the resultTimeRange is before the first occupancy start time if so call this!!!
                 if(resultTimeRange.begin().isBefore(mergedTimeIntervalsToKeep.get(0).begin())){
                     // then remove records from the video result time start til our first alarming occupancy
                     numObsRemoved += db.getObservationStore().removeEntries(new ObsFilter.Builder()
                             .withDataStreams(dsID)
                             .withResultTimeDuring(resultTimeRange.begin(), mergedTimeIntervalsToKeep.get(0).begin())
                             .build());
                 }

                 // now purge anything after the last item in the interval and the end of the result time of the video
                 var lastIndex = mergedTimeIntervalsToKeep.size()-1;

                 if(resultTimeRange.end().isAfter(mergedTimeIntervalsToKeep.get(lastIndex).end())){

                     Instant start = mergedTimeIntervalsToKeep.get(lastIndex).end();
                     Instant end = resultTimeRange.end();
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

                     if(currentIntervalEndTime.isBefore(nextIntervalStartTime)){
                         numObsRemoved += db.getObservationStore().removeEntries(new ObsFilter.Builder()
                                 .withDataStreams(dsID)
                                 .withResultTimeDuring(currentIntervalEndTime, nextIntervalStartTime)
                                 .build());
                     }
                     i++;
                 }


             }




        }
        if (log.isInfoEnabled()) {

            log.info("Purging Data. Removed records: {}", numObsRemoved);
        }
    }
}

