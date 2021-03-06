/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.crunch.types;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;

import com.google.common.collect.Maps;

public class MapDeepCopier<T> implements DeepCopier<Map<String, T>> {

  private final PType<T> ptype;

  public MapDeepCopier(PType<T> ptype) {
    this.ptype = ptype;
  }

  @Override
  public void initialize(Configuration conf) {
    this.ptype.initialize(conf);
  }

  @Override
  public Map<String, T> deepCopy(Map<String, T> source) {
    if (source == null) {
      return null;
    }
    
    Map<String, T> deepCopyMap = Maps.newHashMap();
    for (Entry<String, T> entry : source.entrySet()) {
      deepCopyMap.put(entry.getKey(), ptype.getDetachedValue(entry.getValue()));
    }
    return deepCopyMap;

  }

}
