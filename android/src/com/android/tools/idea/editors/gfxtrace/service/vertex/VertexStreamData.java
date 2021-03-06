/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.idea.editors.gfxtrace.service.vertex;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.*;
import com.android.tools.rpclib.schema.*;
import com.android.tools.rpclib.any.Box;

import java.io.IOException;

public final class VertexStreamData implements BinaryObject {
  public static final VertexStreamData NO_DATA = new VertexStreamData();

  //<<<Start:Java.ClassBody:1>>>
  private Format myFormat;
  private Object myData;

  // Constructs a default-initialized {@link VertexStreamData}.
  public VertexStreamData() {}


  public Format getFormat() {
    return myFormat;
  }

  public VertexStreamData setFormat(Format v) {
    myFormat = v;
    return this;
  }

  public Object getData() {
    return myData;
  }

  public VertexStreamData setData(Object v) {
    myData = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("vertex", "StreamData", "", "");

  static {
    ENTITY.setFields(new Field[]{
      new Field("Format", new Interface("Format")),
      new Field("Data", new AnyType()),
    });
    Namespace.register(Klass.INSTANCE);
  }
  public static void register() {}
  //<<<End:Java.ClassBody:1>>>

  public <T> T getDataAndCast() {
    return (T)myData;
  }

  public enum Klass implements BinaryClass {
    //<<<Start:Java.KlassBody:2>>>
    INSTANCE;

    @Override @NotNull
    public Entity entity() { return ENTITY; }

    @Override @NotNull
    public BinaryObject create() { return new VertexStreamData(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      VertexStreamData o = (VertexStreamData)obj;
      e.object(o.myFormat.unwrap());
      e.variant(Box.wrap(o.myData));
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      VertexStreamData o = (VertexStreamData)obj;
      o.myFormat = Format.wrap(d.object());
      o.myData = ((Box)d.variant()).unwrap();
    }
    //<<<End:Java.KlassBody:2>>>
  }
}
