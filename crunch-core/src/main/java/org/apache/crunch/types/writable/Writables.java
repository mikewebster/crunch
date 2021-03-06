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
package org.apache.crunch.types.writable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.crunch.CrunchRuntimeException;
import org.apache.crunch.MapFn;
import org.apache.crunch.Pair;
import org.apache.crunch.Tuple;
import org.apache.crunch.Tuple3;
import org.apache.crunch.Tuple4;
import org.apache.crunch.TupleN;
import org.apache.crunch.Union;
import org.apache.crunch.fn.CompositeMapFn;
import org.apache.crunch.fn.IdentityFn;
import org.apache.crunch.types.PType;
import org.apache.crunch.types.PTypes;
import org.apache.crunch.types.TupleFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableFactories;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Defines static methods that are analogous to the methods defined in
 * {@link WritableTypeFamily} for convenient static importing.
 * 
 */
public class Writables {
  private static final MapFn<NullWritable, Void> NULL_WRITABLE_TO_VOID = new MapFn<NullWritable, Void>() {
    @Override
    public Void map(NullWritable input) {
      return null;
    }
  };

  private static final MapFn<Void, NullWritable> VOID_TO_NULL_WRITABLE = new MapFn<Void, NullWritable>() {
    @Override
    public NullWritable map(Void input) {
      return NullWritable.get();
    }
  };

  private static final MapFn<Text, String> TEXT_TO_STRING = new MapFn<Text, String>() {
    @Override
    public String map(Text input) {
      return input.toString();
    }
  };

  private static final MapFn<String, Text> STRING_TO_TEXT = new MapFn<String, Text>() {
    @Override
    public Text map(String input) {
      return new Text(input);
    }
  };

  private static final MapFn<IntWritable, Integer> IW_TO_INT = new MapFn<IntWritable, Integer>() {
    @Override
    public Integer map(IntWritable input) {
      return input.get();
    }
  };

  private static final MapFn<Integer, IntWritable> INT_TO_IW = new MapFn<Integer, IntWritable>() {
    @Override
    public IntWritable map(Integer input) {
      return new IntWritable(input);
    }
  };

  private static final MapFn<LongWritable, Long> LW_TO_LONG = new MapFn<LongWritable, Long>() {
    @Override
    public Long map(LongWritable input) {
      return input.get();
    }
  };

  private static final MapFn<Long, LongWritable> LONG_TO_LW = new MapFn<Long, LongWritable>() {
    @Override
    public LongWritable map(Long input) {
      return new LongWritable(input);
    }
  };

  private static final MapFn<FloatWritable, Float> FW_TO_FLOAT = new MapFn<FloatWritable, Float>() {
    @Override
    public Float map(FloatWritable input) {
      return input.get();
    }
  };

  private static final MapFn<Float, FloatWritable> FLOAT_TO_FW = new MapFn<Float, FloatWritable>() {
    @Override
    public FloatWritable map(Float input) {
      return new FloatWritable(input);
    }
  };

  private static final MapFn<DoubleWritable, Double> DW_TO_DOUBLE = new MapFn<DoubleWritable, Double>() {
    @Override
    public Double map(DoubleWritable input) {
      return input.get();
    }
  };

  private static final MapFn<Double, DoubleWritable> DOUBLE_TO_DW = new MapFn<Double, DoubleWritable>() {
    @Override
    public DoubleWritable map(Double input) {
      return new DoubleWritable(input);
    }
  };

  private static final MapFn<BooleanWritable, Boolean> BW_TO_BOOLEAN = new MapFn<BooleanWritable, Boolean>() {
    @Override
    public Boolean map(BooleanWritable input) {
      return input.get();
    }
  };

  private static final BooleanWritable TRUE = new BooleanWritable(true);
  private static final BooleanWritable FALSE = new BooleanWritable(false);
  private static final MapFn<Boolean, BooleanWritable> BOOLEAN_TO_BW = new MapFn<Boolean, BooleanWritable>() {
    @Override
    public BooleanWritable map(Boolean input) {
      return input == Boolean.TRUE ? TRUE : FALSE;
    }
  };

  private static final MapFn<BytesWritable, ByteBuffer> BW_TO_BB = new MapFn<BytesWritable, ByteBuffer>() {
    @Override
    public ByteBuffer map(BytesWritable input) {
      return ByteBuffer.wrap(input.getBytes(), 0, input.getLength());
    }
  };

  private static final MapFn<ByteBuffer, BytesWritable> BB_TO_BW = new MapFn<ByteBuffer, BytesWritable>() {
    @Override
    public BytesWritable map(ByteBuffer input) {
      BytesWritable bw = new BytesWritable();
      bw.set(input.array(), input.arrayOffset(), input.limit());
      return bw;
    }
  };

  private static <S, W extends Writable> WritableType<S, W> create(Class<S> typeClass, Class<W> writableClass,
      MapFn<W, S> inputDoFn, MapFn<S, W> outputDoFn) {
    return new WritableType<S, W>(typeClass, writableClass, inputDoFn, outputDoFn);
  }

  private static final WritableType<Void, NullWritable> nulls = create(Void.class, NullWritable.class,
      NULL_WRITABLE_TO_VOID, VOID_TO_NULL_WRITABLE);
  private static final WritableType<String, Text> strings = create(String.class, Text.class, TEXT_TO_STRING,
      STRING_TO_TEXT);
  private static final WritableType<Long, LongWritable> longs = create(Long.class, LongWritable.class, LW_TO_LONG,
      LONG_TO_LW);
  private static final WritableType<Integer, IntWritable> ints = create(Integer.class, IntWritable.class, IW_TO_INT,
      INT_TO_IW);
  private static final WritableType<Float, FloatWritable> floats = create(Float.class, FloatWritable.class,
      FW_TO_FLOAT, FLOAT_TO_FW);
  private static final WritableType<Double, DoubleWritable> doubles = create(Double.class, DoubleWritable.class,
      DW_TO_DOUBLE, DOUBLE_TO_DW);
  private static final WritableType<Boolean, BooleanWritable> booleans = create(Boolean.class, BooleanWritable.class,
      BW_TO_BOOLEAN, BOOLEAN_TO_BW);
  private static final WritableType<ByteBuffer, BytesWritable> bytes = create(ByteBuffer.class, BytesWritable.class,
      BW_TO_BB, BB_TO_BW);

  private static final Map<Class<?>, PType<?>> PRIMITIVES = ImmutableMap.<Class<?>, PType<?>> builder()
      .put(String.class, strings).put(Long.class, longs).put(Integer.class, ints).put(Float.class, floats)
      .put(Double.class, doubles).put(Boolean.class, booleans).put(ByteBuffer.class, bytes).build();

  private static final Map<Class<?>, WritableType<?, ?>> EXTENSIONS = Maps.newHashMap();

  public static <T> PType<T> getPrimitiveType(Class<T> clazz) {
    return (PType<T>) PRIMITIVES.get(clazz);
  }

  public static <T> void register(Class<T> clazz, WritableType<T, ? extends Writable> ptype) {
    EXTENSIONS.put(clazz, ptype);
  }

  public static final WritableType<Void, NullWritable> nulls() {
    return nulls;
  }

  public static final WritableType<String, Text> strings() {
    return strings;
  }

  public static final WritableType<Long, LongWritable> longs() {
    return longs;
  }

  public static final WritableType<Integer, IntWritable> ints() {
    return ints;
  }

  public static final WritableType<Float, FloatWritable> floats() {
    return floats;
  }

  public static final WritableType<Double, DoubleWritable> doubles() {
    return doubles;
  }

  public static final WritableType<Boolean, BooleanWritable> booleans() {
    return booleans;
  }

  public static final WritableType<ByteBuffer, BytesWritable> bytes() {
    return bytes;
  }

  public static final <T, W extends Writable> WritableType<T, W> records(Class<T> clazz) {
    if (EXTENSIONS.containsKey(clazz)) {
      return (WritableType<T, W>) EXTENSIONS.get(clazz);
    }
    if (Writable.class.isAssignableFrom(clazz)) {
      return (WritableType<T, W>) writables(clazz.asSubclass(Writable.class));
    } else {
      throw new IllegalArgumentException(
          "Cannot create Writable records from non-Writable class"+ clazz.getCanonicalName());
    }
  }

  public static <W extends Writable> WritableType<W, W> writables(Class<W> clazz) {
    MapFn wIdentity = IdentityFn.getInstance();
    return new WritableType<W, W>(clazz, clazz, wIdentity, wIdentity);
  }

  public static <K, V> WritableTableType<K, V> tableOf(PType<K> key, PType<V> value) {
    if (key instanceof WritableTableType) {
      WritableTableType wtt = (WritableTableType) key;
      key = pairs(wtt.getKeyType(), wtt.getValueType());
    } else if (!(key instanceof WritableType)) {
      throw new IllegalArgumentException("Key type must be of class WritableType");
    }
    if (value instanceof WritableTableType) {
      WritableTableType wtt = (WritableTableType) value;
      value = pairs(wtt.getKeyType(), wtt.getValueType());
    } else if (!(value instanceof WritableType)) {
      throw new IllegalArgumentException("Value type must be of class WritableType");
    }
    return new WritableTableType((WritableType) key, (WritableType) value);
  }

  private static <W extends Writable> W create(Class<W> clazz, BytesWritable bytes) {
    W instance = (W) WritableFactories.newInstance(clazz);
    try {
      instance.readFields(new DataInputStream(new ByteArrayInputStream(bytes.getBytes())));
    } catch (IOException e) {
      throw new CrunchRuntimeException(e);
    }
    return instance;
  }

  /**
   * For mapping from {@link TupleWritable} instances to {@link Tuple}s.
   * 
   */
  private static class TWTupleMapFn extends MapFn<TupleWritable, Tuple> {
    private final TupleFactory<?> tupleFactory;
    private final List<MapFn> fns;
    private final List<Class<Writable>> writableClasses;
    
    private transient Object[] values;

    public TWTupleMapFn(TupleFactory<?> tupleFactory, WritableType<?, ?>... ptypes) {
      this.tupleFactory = tupleFactory;
      this.fns = Lists.newArrayList();
      this.writableClasses = Lists.newArrayList();
      for (WritableType ptype : ptypes) {
        fns.add(ptype.getInputMapFn());
        writableClasses.add(ptype.getSerializationClass());
      }
    }

    @Override
    public void configure(Configuration conf) {
      for (MapFn fn : fns) {
        fn.configure(conf);
      }
    }

    @Override
    public void setContext(TaskInputOutputContext<?, ?, ?, ?> context) {
      for (MapFn fn : fns) {
        fn.setContext(context);
      }
    }
    
    @Override
    public void initialize() {
      for (MapFn fn : fns) {
        fn.initialize();
      }
      // The rest of the methods allocate new
      // objects each time. However this one
      // uses Tuple.tuplify which does a copy
      this.values = new Object[fns.size()];
      tupleFactory.initialize();
    }

    @Override
    public Tuple map(TupleWritable in) {
      for (int i = 0; i < values.length; i++) {
        if (in.has(i)) {
          Writable w = create(writableClasses.get(i), in.get(i));
          values[i] = fns.get(i).map(w);
        } else {
          values[i] = null;
        }
      }
      return tupleFactory.makeTuple(values);
    }
  }

  /**
   * For mapping from {@code Tuple}s to {@code TupleWritable}s.
   * 
   */
  private static class TupleTWMapFn extends MapFn<Tuple, TupleWritable> {

    private transient TupleWritable writable;
    private transient BytesWritable[] values;

    private final List<MapFn> fns;
    private final List<Class<Writable>> writableClasses;
    
    public TupleTWMapFn(PType<?>... ptypes) {
      this.fns = Lists.newArrayList();
      this.writableClasses = Lists.newArrayList();
      for (PType<?> ptype : ptypes) {
        fns.add(ptype.getOutputMapFn());
        writableClasses.add(((WritableType) ptype).getSerializationClass());
      }
    }

    @Override
    public void configure(Configuration conf) {
      for (MapFn fn : fns) {
        fn.configure(conf);
      }
    }

    @Override
    public void setContext(TaskInputOutputContext<?, ?, ?, ?> context) {
      for (MapFn fn : fns) {
        fn.setContext(context);
      }
    }
    
    @Override
    public void initialize() {
      this.values = new BytesWritable[fns.size()];
      this.writable = new TupleWritable(values);
      this.writable.setWritableClasses(writableClasses);
      for (MapFn fn : fns) {
        fn.initialize();
      }
    }

    @Override
    public TupleWritable map(Tuple input) {
      writable.clearWritten();
      for (int i = 0; i < input.size(); i++) {
        Object value = input.get(i);
        if (value != null) {
          writable.setWritten(i);
          Writable w = (Writable) fns.get(i).map(value);
          values[i] = new BytesWritable(WritableUtils.toByteArray(w));
        }
      }
      return writable;
    }
  }

  public static <V1, V2> WritableType<Pair<V1, V2>, TupleWritable> pairs(PType<V1> p1, PType<V2> p2) {
    TWTupleMapFn input = new TWTupleMapFn(TupleFactory.PAIR, (WritableType) p1, (WritableType) p2);
    TupleTWMapFn output = new TupleTWMapFn(p1, p2);
    return new WritableType(Pair.class, TupleWritable.class, input, output, p1, p2);
  }

  public static <V1, V2, V3> WritableType<Tuple3<V1, V2, V3>, TupleWritable> triples(PType<V1> p1, PType<V2> p2,
      PType<V3> p3) {
    TWTupleMapFn input = new TWTupleMapFn(TupleFactory.TUPLE3, (WritableType) p1,
        (WritableType) p2, (WritableType) p3);
    TupleTWMapFn output = new TupleTWMapFn(p1, p2, p3);
    return new WritableType(Tuple3.class, TupleWritable.class, input, output, p1, p2, p3);
  }

  public static <V1, V2, V3, V4> WritableType<Tuple4<V1, V2, V3, V4>, TupleWritable> quads(PType<V1> p1, PType<V2> p2,
      PType<V3> p3, PType<V4> p4) {
    TWTupleMapFn input = new TWTupleMapFn(TupleFactory.TUPLE4, (WritableType) p1,
        (WritableType) p2, (WritableType) p3, (WritableType) p4);
    TupleTWMapFn output = new TupleTWMapFn(p1, p2, p3, p4);
    return new WritableType(Tuple4.class, TupleWritable.class, input, output, p1, p2, p3, p4);
  }

  public static WritableType<TupleN, TupleWritable> tuples(PType... ptypes) {
    WritableType[] wt = new WritableType[ptypes.length];
    for (int i = 0; i < wt.length; i++) {
      wt[i] = (WritableType) ptypes[i];
    }
    TWTupleMapFn input = new TWTupleMapFn(TupleFactory.TUPLEN, wt);
    TupleTWMapFn output = new TupleTWMapFn(ptypes);
    return new WritableType(TupleN.class, TupleWritable.class, input, output, ptypes);
  }

  public static <T extends Tuple> PType<T> tuples(Class<T> clazz, PType... ptypes) {
    Class[] typeArgs = new Class[ptypes.length];
    WritableType[] wt = new WritableType[ptypes.length];
    for (int i = 0; i < typeArgs.length; i++) {
      typeArgs[i] = ptypes[i].getTypeClass();
      wt[i] = (WritableType) ptypes[i];
    }
    TupleFactory<T> factory = TupleFactory.create(clazz, typeArgs);
    TWTupleMapFn input = new TWTupleMapFn(factory, wt);
    TupleTWMapFn output = new TupleTWMapFn(ptypes);
    return new WritableType(clazz, TupleWritable.class, input, output, ptypes);
  }

  /**
   * For mapping from {@link TupleWritable} instances to {@link Tuple}s.
   *
   */
  private static class UWInputFn extends MapFn<UnionWritable, Union> {
    private final List<MapFn> fns;
    private final List<Class<Writable>> writableClasses;

    public UWInputFn(WritableType<?, ?>... ptypes) {
      this.fns = Lists.newArrayList();
      this.writableClasses = Lists.newArrayList();
      for (WritableType ptype : ptypes) {
        fns.add(ptype.getInputMapFn());
        writableClasses.add(ptype.getSerializationClass());
      }
    }

    @Override
    public void configure(Configuration conf) {
      for (MapFn fn : fns) {
        fn.configure(conf);
      }
    }

    @Override
    public void setContext(TaskInputOutputContext<?, ?, ?, ?> context) {
      for (MapFn fn : fns) {
        fn.setContext(context);
      }
    }

    @Override
    public void initialize() {
      for (MapFn fn : fns) {
        fn.initialize();
      }
    }

    @Override
    public Union map(UnionWritable in) {
      int index = in.getIndex();
      Writable w = create(writableClasses.get(index), in.getValue());
      return new Union(index, fns.get(index).map(w));
    }
  }

  /**
   * For mapping from {@code Tuple}s to {@code TupleWritable}s.
   *
   */
  private static class UWOutputFn extends MapFn<Union, UnionWritable> {

    private final List<MapFn> fns;

    public UWOutputFn(PType<?>... ptypes) {
      this.fns = Lists.newArrayList();
      for (PType<?> ptype : ptypes) {
        fns.add(ptype.getOutputMapFn());
      }
    }

    @Override
    public void configure(Configuration conf) {
      for (MapFn fn : fns) {
        fn.configure(conf);
      }
    }

    @Override
    public void setContext(TaskInputOutputContext<?, ?, ?, ?> context) {
      for (MapFn fn : fns) {
        fn.setContext(context);
      }
    }

    @Override
    public void initialize() {
      for (MapFn fn : fns) {
        fn.initialize();
      }
    }

    @Override
    public UnionWritable map(Union input) {
      int index = input.getIndex();
      Writable w = (Writable) fns.get(index).map(input.getValue());
      return new UnionWritable(index, new BytesWritable(WritableUtils.toByteArray(w)));
    }
  }

  public static PType<Union> unionOf(PType<?>... ptypes) {
    WritableType[] wt = new WritableType[ptypes.length];
    for (int i = 0; i < wt.length; i++) {
      wt[i] = (WritableType) ptypes[i];
    }
    UWInputFn input= new UWInputFn(wt);
    UWOutputFn output = new UWOutputFn(ptypes);
    return new WritableType(Union.class, UnionWritable.class, input, output, ptypes);
  }

  public static <S, T> PType<T> derived(Class<T> clazz, MapFn<S, T> inputFn, MapFn<T, S> outputFn, PType<S> base) {
    WritableType<S, ?> wt = (WritableType<S, ?>) base;
    MapFn input = new CompositeMapFn(wt.getInputMapFn(), inputFn);
    MapFn output = new CompositeMapFn(outputFn, wt.getOutputMapFn());
    return new WritableType(clazz, wt.getSerializationClass(), input, output, base.getSubTypes().toArray(new PType[0]));
  }

  private static class ArrayCollectionMapFn<T> extends MapFn<GenericArrayWritable, Collection<T>> {
    private Class<Writable> clazz;
    private final MapFn<Object, T> mapFn;
    
    public ArrayCollectionMapFn(Class<Writable> clazz, MapFn<Object, T> mapFn) {
      this.clazz = clazz;
      this.mapFn = mapFn;
    }

    @Override
    public void configure(Configuration conf) {
      mapFn.configure(conf);
    }

    @Override
    public void setContext(TaskInputOutputContext<?, ?, ?, ?> context) {
      mapFn.setContext(context);
    }
    
    @Override
    public void initialize() {
      mapFn.initialize();
    }

    @Override
    public Collection<T> map(GenericArrayWritable input) {
      Collection<T> collection = Lists.newArrayList();
      for (BytesWritable raw : input.get()) {
        Writable w = create(clazz, raw);
        collection.add(mapFn.map(w));
      }
      return collection;
    }
  }

  private static class CollectionArrayMapFn<T> extends MapFn<Collection<T>, GenericArrayWritable> {

    private final MapFn<T, Object> mapFn;

    public CollectionArrayMapFn(MapFn<T, Object> mapFn) {
      this.mapFn = mapFn;
    }

    @Override
    public void configure(Configuration conf) {
      mapFn.configure(conf);
    }

    @Override
    public void setContext(TaskInputOutputContext<?, ?, ?, ?> context) {
      mapFn.setContext(context);
    }
    
    @Override
    public void initialize() {
      mapFn.initialize();
    }

    @Override
    public GenericArrayWritable map(Collection<T> input) {
      GenericArrayWritable arrayWritable = new GenericArrayWritable();
      BytesWritable[] w = new BytesWritable[input.size()];
      int index = 0;
      for (T in : input) {
        Writable v = (Writable) mapFn.map(in);
        w[index++] = new BytesWritable(WritableUtils.toByteArray(v));
      }
      arrayWritable.set(w);
      return arrayWritable;
    }
  }

  public static <T> WritableType<Collection<T>, GenericArrayWritable> collections(PType<T> ptype) {
    WritableType<T, ?> wt = (WritableType<T, ?>) ptype;
    return new WritableType(Collection.class, GenericArrayWritable.class,
        new ArrayCollectionMapFn(wt.getSerializationClass(), wt.getInputMapFn()),
        new CollectionArrayMapFn(wt.getOutputMapFn()), ptype);
  }

  private static class MapInputMapFn<T> extends MapFn<TextMapWritable, Map<String, T>> {
    private final Class<Writable> clazz;
    private final MapFn<Writable, T> mapFn;

    public MapInputMapFn(Class<Writable> clazz, MapFn<Writable, T> mapFn) {
      this.clazz = clazz;
      this.mapFn = mapFn;
    }

    @Override
    public void configure(Configuration conf) {
      mapFn.configure(conf);
    }

    @Override
    public void setContext(TaskInputOutputContext<?, ?, ?, ?> context) {
      mapFn.setContext(context);
    }
    
    @Override
    public void initialize() {
      mapFn.initialize();
    }

    @Override
    public Map<String, T> map(TextMapWritable input) {
      Map<String, T> out = Maps.newHashMap();
      for (Map.Entry<Text, BytesWritable> e : input.entrySet()) {
        Writable v = create(clazz, e.getValue());
        out.put(e.getKey().toString(), mapFn.map(v));
      }
      return out;
    }
  }

  private static class MapOutputMapFn<T> extends MapFn<Map<String, T>, TextMapWritable> {

    private final MapFn<T, Writable> mapFn;

    public MapOutputMapFn(MapFn<T, Writable> mapFn) {
      this.mapFn = mapFn;
    }

    @Override
    public void configure(Configuration conf) {
      mapFn.configure(conf);
    }

    @Override
    public void setContext(TaskInputOutputContext<?, ?, ?, ?> context) {
      mapFn.setContext(context);
    }
    
    @Override
    public void initialize() {
      mapFn.initialize();
    }

    @Override
    public TextMapWritable map(Map<String, T> input) {
      TextMapWritable tmw = new TextMapWritable();
      for (Map.Entry<String, T> e : input.entrySet()) {
        Writable w = mapFn.map(e.getValue());
        tmw.put(new Text(e.getKey()), new BytesWritable(WritableUtils.toByteArray(w)));
      }
      return tmw;
    }
  }

  public static <T> WritableType<Map<String, T>, MapWritable> maps(PType<T> ptype) {
    WritableType<T, ?> wt = (WritableType<T, ?>) ptype;
    return new WritableType(Map.class, TextMapWritable.class,
        new MapInputMapFn(wt.getSerializationClass(), wt.getInputMapFn()),
        new MapOutputMapFn(wt.getOutputMapFn()), ptype);
  }

  public static <T> PType<T> jsons(Class<T> clazz) {
    return PTypes.jsonString(clazz, WritableTypeFamily.getInstance());
  }

  // Not instantiable
  private Writables() {
  }
}
