/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

 package io.cdap.directives.column;

 import io.cdap.wrangler.api.Arguments;
 import io.cdap.wrangler.api.Directive;
 import io.cdap.wrangler.api.ExecutorContext;
 import io.cdap.wrangler.api.Row;
 import io.cdap.wrangler.api.parser.ByteSize;
 import io.cdap.wrangler.api.parser.TimeDuration;
 import io.cdap.wrangler.api.parser.Token;
 
 import io.cdap.wrangler.api.parser.UsageDefinition;
 
 import java.util.Collections;
 import java.util.List;
 /**
  * A directive that aggregates total byte size and time duration from input rows.
  *
  * <p>This directive processes all rows and calculates the cumulative sum of values
  * from two specified columns: one representing sizes (e.g., file sizes, memory usage)
  * and the other representing durations (e.g., execution time, latency).
  * It supports inputs in both numeric format (e.g., <code>1024</code>) and string format
  * (e.g., <code>"1KB"</code>, <code>"2s"</code>) and converts them to bytes and milliseconds respectively.</p>
  *
  * <p>The directive then emits a single row with the total size and duration,
  * storing them in user-defined output columns.</p>
  *
  * <b>Usage:</b>
  * <pre>
  * aggregate-stats sizeColumn durationColumn outputSizeColumn outputDurationColumn
  * </pre>
  *
  * <b>Example:</b>
  * <pre>
  * aggregate-stats file_size execution_time total_size total_time
  * </pre>
  *
  * <b>Arguments:</b>
  * <ul>
  *   <li>0 - Name of the column containing the byte sizes (String or Long)</li>
  *   <li>1 - Name of the column containing the durations (String or Long)</li>
  *   <li>2 - Name of the output column to store total byte size (Long)</li>
  *   <li>3 - Name of the output column to store total duration (Long, in milliseconds)</li>
  * </ul>
  *
  * @since 4.12.0
  */
 
 public class AggregateStats implements Directive {
 
   private String sizeColumn;
   private String durationColumn;
   private String outputSizeColumn;
   private String outputTimeColumn;
 
   @Override
   public UsageDefinition define() {
     return UsageDefinition.builder("aggregate-stats").build();
   }
 
   @Override
   public void initialize(Arguments arguments) {
     sizeColumn = arguments.<Token>value("0").value().toString();
     durationColumn = arguments.<Token>value("1").value().toString();
     outputSizeColumn = arguments.<Token>value("2").value().toString();
     outputTimeColumn = arguments.<Token>value("3").value().toString();
   }
 
   @Override
   public List<Row> execute(List<Row> rows, ExecutorContext context) {
     long totalBytes = 0L;
     long totalMillis = 0L;
 
     for (Row row : rows) {
       Object sizeVal = row.getValue(sizeColumn);
       Object timeVal = row.getValue(durationColumn);
 
       if (sizeVal instanceof Long) {
         totalBytes += (Long) sizeVal;
       } else if (sizeVal instanceof String) {
         totalBytes += new ByteSize((String) sizeVal).getBytes();
       }
 
       if (timeVal instanceof Long) {
         totalMillis += (Long) timeVal;
       } else if (timeVal instanceof String) {
         totalMillis += new TimeDuration((String) timeVal).getMilliseconds();
       }
     }
 
     Row result = new Row();
     result.add(outputSizeColumn, totalBytes);
     result.add(outputTimeColumn, totalMillis);
 
     return Collections.singletonList(result);
   }
 
   @Override
   public void destroy() {
     // Nothing to clean up
   }
 }
 