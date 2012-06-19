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
package com.cloudera.crunch.types;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import com.cloudera.crunch.Pair;
import com.cloudera.crunch.Tuple;
import com.cloudera.crunch.Tuple3;
import com.cloudera.crunch.Tuple4;
import com.cloudera.crunch.TupleN;
import com.cloudera.crunch.impl.mr.run.CrunchRuntimeException;

public abstract class TupleFactory<T extends Tuple> implements Serializable {
  private static final long serialVersionUID = 1L;
  
  public void initialize() { }
  
  public abstract T makeTuple(Object...values);
  
  public static final TupleFactory<Pair> PAIR = new TupleFactory<Pair>() {
    @Override
    public Pair makeTuple(Object... values) {
      return Pair.of(values[0], values[1]);
    }
  };

  public static final TupleFactory<Tuple3> TUPLE3 = new TupleFactory<Tuple3>() {
    @Override
    public Tuple3 makeTuple(Object... values) {
      return Tuple3.of(values[0], values[1], values[2]);
    }
  };
  
  public static final TupleFactory<Tuple4> TUPLE4 = new TupleFactory<Tuple4>() {
    @Override
    public Tuple4 makeTuple(Object... values) {
      return Tuple4.of(values[0], values[1], values[2], values[3]);
    }
  };

  public static final TupleFactory<TupleN> TUPLEN = new TupleFactory<TupleN>() {
    @Override
    public TupleN makeTuple(Object... values) {
      return new TupleN(values);
    }
  };

  public static <T extends Tuple> TupleFactory<T> create(Class<T> clazz, Class... typeArgs) {
    return new CustomTupleFactory<T>(clazz, typeArgs);
  }
  
  private static class CustomTupleFactory<T extends Tuple> extends TupleFactory<T> {

    private final Class<T> clazz;
    private final Class[] typeArgs;
    
    private transient Constructor<T> constructor;
    
    public CustomTupleFactory(Class<T> clazz, Class[] typeArgs) {
      this.clazz = clazz;
      this.typeArgs = typeArgs;
    }
    
    @Override
    public void initialize() {
      try {
        constructor = clazz.getConstructor(typeArgs);
      } catch (Exception e) {
        throw new CrunchRuntimeException(e);
      }
    }
    
    @Override
    public T makeTuple(Object... values) {
      try {
        return constructor.newInstance(values);
      } catch (Exception e) {
        throw new CrunchRuntimeException(e);
      }
    }
  }
}
