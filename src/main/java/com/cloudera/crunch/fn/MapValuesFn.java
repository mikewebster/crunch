/**
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.crunch.fn;

import com.cloudera.crunch.DoFn;
import com.cloudera.crunch.Emitter;
import com.cloudera.crunch.Pair;

public abstract class MapValuesFn<K, V1, V2> extends DoFn<Pair<K, V1>, Pair<K, V2>> {
  private static final long serialVersionUID = 1L;
  
  @Override
  public void process(Pair<K, V1> input, Emitter<Pair<K, V2>> emitter) {
    emitter.emit(Pair.of(input.first(), map(input.second())));
  }

  public abstract V2 map(V1 v);
}
